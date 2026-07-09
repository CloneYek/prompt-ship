package com.xiaoyu.promptship.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xiaoyu.promptship.ai.AiCodeGeneratorServiceFactory;
import com.xiaoyu.promptship.constant.AppConstant;
import com.xiaoyu.promptship.core.AiCodeGeneratorFacade;
import com.xiaoyu.promptship.core.vue.VueProjectBuilder;
import com.xiaoyu.promptship.core.vue.VueSkeletonCopier;
import com.xiaoyu.promptship.exception.BusinessException;
import com.xiaoyu.promptship.exception.ErrorCode;
import com.xiaoyu.promptship.exception.ThrowUtils;
import com.xiaoyu.promptship.mapper.AppMapper;
import com.xiaoyu.promptship.model.dto.AppChatContinueRequest;
import com.xiaoyu.promptship.model.dto.AppCreateRequest;
import com.xiaoyu.promptship.model.dto.AppQueryRequest;
import com.xiaoyu.promptship.model.dto.AppUpdateMyRequest;
import com.xiaoyu.promptship.model.dto.AppUpdateRequest;
import com.xiaoyu.promptship.model.entity.App;
import com.xiaoyu.promptship.model.entity.ChatHistory;
import com.xiaoyu.promptship.model.entity.User;
import com.xiaoyu.promptship.model.enums.ChatHistoryRoleEnum;
import com.xiaoyu.promptship.model.enums.CodeGenTypeEnum;
import com.xiaoyu.promptship.model.vo.AppVO;
import com.xiaoyu.promptship.service.AppService;
import com.xiaoyu.promptship.service.ChatHistoryService;
import com.xiaoyu.promptship.service.ScreenshotService;
import com.xiaoyu.promptship.service.UserService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 应用 服务层实现。
 *
 * @author xiaoyu
 * @since 1.0
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private VueSkeletonCopier vueSkeletonCopier;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ScreenshotService screenshotService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建应用（用户用）
     *
     * @param request     创建请求
     * @param httpRequest HTTP 请求
     * @return 新应用 id
     */
    @Override
    public long createApp(AppCreateRequest request, HttpServletRequest httpRequest) {
        User currentUser = userService.getLoginUser(httpRequest);

        App app = new App();
        //暂时默认appName为提示词的前12位
        String appName = CharSequenceUtil.isNotBlank(request.getAppName())
                ? request.getAppName()
                : CharSequenceUtil.subPre(request.getInitPrompt(), 12);
        app.setAppName(appName);
        app.setInitPrompt(request.getInitPrompt());
        app.setUserId(currentUser.getId());
        app.setCover(AppConstant.DEFAULT_APP_COVER);
        // 默认生成 Vue 工程化项目
        app.setCodeGenType(CodeGenTypeEnum.VUE_APP.getValue());
        // 暂时默认优先级为普通
        app.setPriority(0);

        boolean saved = this.save(app);
        ThrowUtils.throwIf(!saved, new BusinessException(ErrorCode.SYSTEM_ERROR, "创建失败，请稍后重试"));

        return app.getId();
    }

    /**
     * 创建应用并与 AI 对话生成代码（流式）
     * <p>
     * 流程：创建应用 → 保存用户消息到对话历史 → AI 流式生成（携带历史上下文）
     * → 收集完整 AI 回复 → 保存 AI 回复到对话历史。
     * </p>
     * <p>
     * SSE 事件格式：
     * <ul>
     *   <li>{@code {"i":123}} — 首个事件，纯数字为 appId</li>
     *   <li>{@code {"d":"..."}} — 代码生成块（token 级）</li>
     *   <li>{@code event:done} — 流正常结束（Controller 层发送）</li>
     * </ul>
     * 使用单字母 key 以减少网络传输开销。
     *
     * @param request     创建请求（提示词、应用名称）
     * @param httpRequest HTTP 请求
     * @return 流式 JSON 事件序列
     */
    @Override
    public Flux<String> chatToGenCode(AppCreateRequest request, HttpServletRequest httpRequest) {
        User currentUser = userService.getLoginUser(httpRequest);

        // 1. 创建应用，获得 appId
        long appId = this.createApp(request, httpRequest);

        // 2. 保存用户消息到对话历史
        saveChatMessage(appId, currentUser.getId(), ChatHistoryRoleEnum.USER, request.getInitPrompt());

        // 3. AI 流式生成代码，使用 per-app Service 携带历史上下文
        Flux<String> aiFlux = aiCodeGeneratorFacade.generateAndSaveCodeStreamForApp(
                request.getInitPrompt(), CodeGenTypeEnum.MULTI_FILE, appId);

        // 4. 收集完整 AI 回复，流结束后保存到对话历史
        StringBuilder aiResponseBuilder = new StringBuilder();

        return Flux.concat(
                Flux.just(buildInit(appId)),
                aiFlux
                        .doOnNext(aiResponseBuilder::append)
                        .map(this::buildChunk)
                        .doOnComplete(() -> saveChatMessage(
                                appId, currentUser.getId(), ChatHistoryRoleEnum.ASSISTANT,
                                aiResponseBuilder.toString()))
        );
    }

    /**
     * 基于已有应用继续对话（流式）。
     * <p>
     * 流程：校验权限 → 保存用户新消息 → AI 流式生成（携带该 App 的完整历史上下文）
     * → 收集完整 AI 回复 → 保存到对话历史。
     * </p>
     *
     * @param request     续聊请求（appId、新消息）
     * @param httpRequest HTTP 请求
     * @return 流式 JSON 事件序列
     */
    @Override
    public Flux<String> chatContinue(AppChatContinueRequest request, HttpServletRequest httpRequest) {
        User currentUser = userService.getLoginUser(httpRequest);
        Long appId = request.getAppId();

        // 1. 校验应用存在且属于当前用户
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!app.getUserId().equals(currentUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权操作该应用");

        // 2. 保存用户新消息到对话历史
        saveChatMessage(appId, currentUser.getId(), ChatHistoryRoleEnum.USER, request.getMessage());

        // 3. 确定代码生成类型（沿用该应用创建时的类型）
        CodeGenTypeEnum codeGenType = CodeGenTypeEnum.getEnumByValue(app.getCodeGenType());
        if (codeGenType == null) {
            codeGenType = CodeGenTypeEnum.MULTI_FILE;
        }

        // 4. AI 流式生成代码，使用 per-app Service 携带历史上下文
        Flux<String> aiFlux = aiCodeGeneratorFacade.generateAndSaveCodeStreamForApp(
                request.getMessage(), codeGenType, appId);

        // 5. 收集完整 AI 回复，流结束后保存到对话历史
        StringBuilder aiResponseBuilder = new StringBuilder();

        return Flux.concat(
                Flux.just(buildInit(appId)),
                aiFlux
                        .doOnNext(aiResponseBuilder::append)
                        .map(this::buildChunk)
                        .doOnComplete(() -> saveChatMessage(
                                appId, currentUser.getId(), ChatHistoryRoleEnum.ASSISTANT,
                                aiResponseBuilder.toString()))
        );
    }

    /**
     * 创建 Vue 工程化应用并流式生成（SSE）。
     * <p>
     * 流程：创建应用(VUE_APP) → 拷贝骨架 → 保存用户消息
     * → AI 工具调用流式生成 → npm install/build → 保存 AI 回复。
     * </p>
     */
    public SseEmitter chatToGenVueApp(AppCreateRequest request, HttpServletRequest httpRequest) {
        //获取当前登录用户信息
        User currentUser = userService.getLoginUser(httpRequest);
        //创建应用
        long appId = this.createApp(request, httpRequest);
        //保存用户消息到数据库
        saveChatMessage(appId, currentUser.getId(), ChatHistoryRoleEnum.USER, request.getInitPrompt());

        try {
            vueSkeletonCopier.copySkeleton(appId);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "项目骨架初始化失败");
        }

        // 获取TokenStream流式对象
        TokenStream stream = aiCodeGeneratorFacade.generateVueAppStream(request.getInitPrompt(), appId);
        // 转成Sse对象
        return bridgeVueTokenStream(stream, appId, currentUser.getId());
    }

    /**
     * 继续 Vue 工程化应用的对话（SSE）。
     */
    public SseEmitter chatContinueVueApp(AppChatContinueRequest request, HttpServletRequest httpRequest) {
        User currentUser = userService.getLoginUser(httpRequest);
        Long appId = request.getAppId();

        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!app.getUserId().equals(currentUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权操作该应用");

        saveChatMessage(appId, currentUser.getId(), ChatHistoryRoleEnum.USER, request.getMessage());

        TokenStream stream = aiCodeGeneratorFacade.generateVueAppStream(request.getMessage(), appId);
        return bridgeVueTokenStream(stream, appId, currentUser.getId());
    }

    /**
     * 继续对话 SSE 流式响应，内部根据 app 的 codeGenType 自动分流。
     */
    @Override
    public SseEmitter chatContinueSse(AppChatContinueRequest request, HttpServletRequest httpRequest) {
        Long appId = request.getAppId();
        App app = this.getById(appId);
        if (app != null && CodeGenTypeEnum.VUE_APP.getValue().equals(app.getCodeGenType())) {
            return chatContinueVueApp(request, httpRequest);
        }

        // 非 Vue 模式：沿用原有 Flux 流程
        SseEmitter emitter = new SseEmitter(600000L);
        Flux<String> flux = chatContinue(request, httpRequest);
        flux.subscribe(
                chunk -> {
                    try {
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                },
                error -> emitter.completeWithError(error),
                () -> {
                    try {
                        emitter.send(SseEmitter.event().name("done").data(""));
                    } catch (Exception ignored) {
                    }
                    emitter.complete();
                }
        );
        return emitter;
    }

    /**
     * 将 Vue TokenStream 桥接到 SseEmitter，统一处理流式事件、工具调用、构建和持久化。
     */
    private SseEmitter bridgeVueTokenStream(TokenStream stream, Long appId, Long userId) {
        SseEmitter emitter = new SseEmitter(600000L);

        safeSend(emitter, SseEmitter.event().data(buildInit(appId)));

        StringBuilder aiResponse = new StringBuilder();
        stream
                // AI 每生成一个文本 token 就回调一次，参数 partial 是增量文本片段
                .onPartialResponse(partial -> {
                    aiResponse.append(partial);
                    safeSend(emitter, SseEmitter.event().data(buildChunk(partial)));
                })
                // 每完成一次工具调用（如 write_file）就回调一次，包含工具名、参数和返回值
                .onToolExecuted(after -> {
                    safeSend(emitter, SseEmitter.event().data(
                            buildToolExecuted(after)));
                })
                // AI 对话完全结束（所有文本生成完毕、所有工具调用完毕）后回调一次
                .onCompleteResponse(resp -> {
                    saveChatMessage(appId, userId, ChatHistoryRoleEnum.ASSISTANT,
                            aiResponse.toString());

                    // 从缓存中读取 AI 声明过的依赖，追加到 package.json 后执行 npm install && npm build
                    var deps = aiCodeGeneratorServiceFactory.getDependencyToolForApp(appId)
                            .getDependencies();
                    var result = vueProjectBuilder.installAndBuild(appId, deps);

                    safeSend(emitter, SseEmitter.event().data(toJson(Map.of(
                            "b", result.success() ? "ok" : "fail",
                            "msg", result.message()))));
                    safeSend(emitter, SseEmitter.event().name("done").data(""));
                    emitter.complete();
                })
                // 流式过程中发生任何错误时回调，将异常传播给 SseEmitter 使其终止 SSE 连接
                .onError(error -> {
                    log.error("Vue 流式生成失败, appId={}", appId, error);
                    emitter.completeWithError(error);
                })
                // 启动异步流式处理。start() 是终点方法，调用后 TokenStream 在后台线程开始工作，
                // 主线程立即返回 emitter 给 Controller → 浏览器建立 SSE 连接
                .start();

        return emitter;
    }

    /**
     * 安全发送 SSE 事件，忽略连接断开异常。
     */
    private void safeSend(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException e) {
            // 连接已断开，忽略
        }
    }

    /**
     * 构建工具执行完成 SSE 事件。
     */
    private String buildToolExecuted(ToolExecution execution) {
        ToolExecutionRequest req = execution.request();
        Map<String, Object> data = new HashMap<>();
        data.put("t", "tool_executed");
        data.put("id", req.id());
        data.put("name", req.name());
        try {
            JsonNode args = objectMapper.readTree(req.arguments());
            if (args.has("path")) {
                data.put("input", Map.of("path", args.get("path").asText()));
            }
        } catch (Exception ignored) {
        }
        return toJson(data);
    }

    /**
     * 保存一条对话消息到 chat_history 表。
     *
     * @param appId   应用 id
     * @param userId  用户 id
     * @param role    消息角色（user / assistant）
     * @param content 消息内容
     */
    private void saveChatMessage(Long appId, Long userId, ChatHistoryRoleEnum role, String content) {
        ChatHistory record = ChatHistory.builder()
                .role(role.getValue())
                .content(content)
                .appId(appId)
                .userId(userId)
                .build();
        boolean saved = chatHistoryService.save(record);
        if (!saved) {
            log.error("保存对话历史失败，appId: {}, role: {}", appId, role.getValue());
        }
        else {
            log.info("保存对话历史成功, appId: {}, role: {}", appId, role.getValue());
        }
    }

    /**
     * 部署应用（用户用）。
     * <ol>
     *   <li>校验应用存在性与用户权限（仅本人可部署）</li>
     *   <li>若已有 deployKey 则直接返回 URL（每个 app 只部署一次）</li>
     *   <li>校验代码是否已生成</li>
     *   <li>生成唯一的 6 位 deployKey（大小写字母 + 数字）</li>
     *   <li>将 code_output 目录下文件复制到 code_deploy/{deployKey}</li>
     *   <li>写入 deployKey 和 deployedTime</li>
     * </ol>
     *
     * @param appId       应用 id
     * @param httpRequest HTTP 请求
     * @return 可公开访问的部署 URL
     */
    @Override
    public String deployApp(Long appId, HttpServletRequest httpRequest) {
        // 1. 获取当前登录用户
        User currentUser = userService.getLoginUser(httpRequest);

        // 2. 查询应用，校验存在性
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);

        // 3. 校验权限：仅本人可以部署
        ThrowUtils.throwIf(!app.getUserId().equals(currentUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权部署该应用");

        // 4. 已有 deployKey 则直接返回（每个 app 只生成一次）
        if (CharSequenceUtil.isNotBlank(app.getDeployKey())) {
            return AppConstant.CODE_DEPLOY_HOST + "/" + app.getDeployKey() + "/";
        }

        // 5. 验证代码是否已生成（检查 code_output 下的临时文件目录）
        String codeDir = AppConstant.CODE_OUTPUT_ROOT_DIR + "/" + app.getCodeGenType() + "_" + appId;
        // Vue 工程化项目部署 dist/ 子目录
        if (CodeGenTypeEnum.VUE_APP.getValue().equals(app.getCodeGenType())) {
            codeDir += "/dist";
        }
        ThrowUtils.throwIf(!FileUtil.exist(codeDir),
                ErrorCode.OPERATION_ERROR, "该应用尚未生成代码，请先生成后再部署");

        // 6. 生成唯一的 6 位 deployKey
        String deployKey = generateUniqueDeployKey();

        // 7. 递归复制源目录内容到部署目录，确保子目录（如 assets/）完整拷贝
        String deployDir = AppConstant.CODE_DEPLOY_ROOT_DIR + "/" + deployKey;
        FileUtil.copyContent(new File(codeDir), new File(deployDir), true);

        // 8. 写入 deployKey 和 deployedTime
        app.setDeployKey(deployKey);
        app.setDeployedTime(LocalDateTime.now());
        boolean updated = this.updateById(app);
        ThrowUtils.throwIf(!updated, new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败，请稍后重试"));

        String deployUrl = AppConstant.CODE_DEPLOY_HOST + "/" + deployKey + "/";

        // 9. 异步生成截图并更新应用封面（使用 Spring Boot 本地端点，不依赖外部 Nginx）
        String screenshotUrl = "http://localhost:8123/api/static/" + deployKey + "/";
        generateAppScreenshotAsync(appId, screenshotUrl);
        return deployUrl;
    }

    /**
     * 异步生成截图并上传
     * @param appId 应用ID
     * @param appUrl 应用URL
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        Thread.startVirtualThread(()->{
            //调用截图服务生成截图并上传
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
            //更新数据库的应用封面
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean result = this.updateById(updateApp);
            ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"更新应用封面失败");
        });
    }

    /**
     * 生成唯一的 6 位 deployKey（大小写字母 + 数字），与数据库已有 key 不重复。
     */
    private String generateUniqueDeployKey() {
        String key;
        int maxRetries = 20;
        do {
            key = RandomUtil.randomString(RandomUtil.BASE_CHAR + RandomUtil.BASE_NUMBER, 6);
            if (--maxRetries < 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成部署标识失败，请稍后重试");
            }
        } while (this.count(new QueryWrapper().eq(App::getDeployKey, key)) > 0);
        return key;
    }

    /**
     * 构建 {"i":appId} 事件
     */
    private String buildInit(long appId) {
        return toJson(Map.of("i", String.valueOf(appId)));
    }

    /**
     * 构建 {"d":"token"} 事件
     */
    private String buildChunk(String token) {
        return toJson(Map.of("d", token));
    }

    /**
     * Map 序列化为单行 JSON
     */
    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("SSE 事件序列化失败", e);
        }
    }

    /**
     * 更新自己的应用（用户用，仅允许修改名称）
     *
     * @param request     更新请求
     * @param httpRequest HTTP 请求
     * @return 脱敏后的应用信息
     */
    @Override
    public AppVO updateApp(AppUpdateMyRequest request, HttpServletRequest httpRequest) {
        User currentUser = userService.getLoginUser(httpRequest);

        App app = this.getById(request.getId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!app.getUserId().equals(currentUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权修改该应用");

        if (CharSequenceUtil.isNotBlank(request.getAppName())) {
            app.setAppName(request.getAppName());
        }
        app.setEditTime(LocalDateTime.now());

        boolean updated = this.updateById(app);
        ThrowUtils.throwIf(!updated, new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败，请稍后重试"));

        return buildAppVO(app);
    }

    /**
     * 删除自己的应用（用户用）
     *
     * @param id          应用 id
     * @param httpRequest HTTP 请求
     * @return 是否成功
     */
    @Override
    public boolean deleteApp(Long id, HttpServletRequest httpRequest) {
        User currentUser = userService.getLoginUser(httpRequest);

        App app = this.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!app.getUserId().equals(currentUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权删除该应用");

        boolean removed = this.removeById(id);
        ThrowUtils.throwIf(!removed, new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败，请稍后重试"));

        return true;
    }

    /**
     * 根据 id 获取应用（脱敏）
     *
     * @param id 应用 id
     * @return 脱敏后的应用信息
     */
    @Override
    public AppVO getAppVOById(Long id) {
        App app = this.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return buildAppVO(app);
    }

    /**
     * 分页获取当前用户的应用列表（脱敏，支持根据名称查询，每页最多 20 个）
     *
     * @param queryRequest 查询请求
     * @param httpRequest  HTTP 请求
     * @return 脱敏后的应用分页数据
     */
    @Override
    public Page<AppVO> listMyAppVOByPage(AppQueryRequest queryRequest, HttpServletRequest httpRequest) {
        User currentUser = userService.getLoginUser(httpRequest);

        // 限制每页最大数量
        int pageSize = Math.min(queryRequest.getPageSize(), AppConstant.MAX_PAGE_SIZE);
        Page<App> page = new Page<>(queryRequest.getPageNum(), pageSize);

        QueryWrapper wrapper = new QueryWrapper()
                .eq(App::getUserId, currentUser.getId());
        if (CharSequenceUtil.isNotBlank(queryRequest.getAppName())) {
            wrapper.like(App::getAppName, queryRequest.getAppName());
        }
        wrapper.orderBy(App::getCreateTime, false);

        Page<App> appPage = this.page(page, wrapper);
        return convertToVOPage(appPage);
    }

    /**
     * 分页获取精选应用列表（脱敏，支持根据名称查询，每页最多 20 个）
     *
     * @param queryRequest 查询请求
     * @return 脱敏后的应用分页数据
     */
    @Override
    public Page<AppVO> listGoodAppVOByPage(AppQueryRequest queryRequest) {
        // 限制每页最大数量
        int pageSize = Math.min(queryRequest.getPageSize(), AppConstant.MAX_PAGE_SIZE);
        Page<App> page = new Page<>(queryRequest.getPageNum(), pageSize);

        QueryWrapper wrapper = new QueryWrapper()
                .ge(App::getPriority, AppConstant.FEATURED_MIN_PRIORITY);
        if (CharSequenceUtil.isNotBlank(queryRequest.getAppName())) {
            wrapper.like(App::getAppName, queryRequest.getAppName());
        }
        wrapper.orderBy(App::getPriority, false)
                .orderBy(App::getCreateTime, false);

        Page<App> appPage = this.page(page, wrapper);
        return convertToVOPage(appPage);
    }

    // region 管理员

    /**
     * 删除应用（管理员用）
     *
     * @param id 应用 id
     * @return 是否成功
     */
    @Override
    public boolean deleteAppByAdmin(Long id) {
        App app = this.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);

        boolean removed = this.removeById(id);
        ThrowUtils.throwIf(!removed, new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败，请稍后重试"));

        return true;
    }

    /**
     * 更新应用（管理员用，支持更新名称、封面、优先级）
     *
     * @param request 更新请求
     * @return 脱敏后的应用信息
     */
    @Override
    public AppVO updateAppByAdmin(AppUpdateRequest request) {
        App app = this.getById(request.getId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);

        if (CharSequenceUtil.isNotBlank(request.getAppName())) {
            app.setAppName(request.getAppName());
        }
        if (CharSequenceUtil.isNotBlank(request.getCover())) {
            app.setCover(request.getCover());
        }
        if (request.getPriority() != null) {
            app.setPriority(request.getPriority());
        }
        app.setEditTime(LocalDateTime.now());

        boolean updated = this.updateById(app);
        ThrowUtils.throwIf(!updated, new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败，请稍后重试"));

        return buildAppVO(app);
    }

    /**
     * 分页获取应用列表（管理员用，支持根据除时间外的字段查询，每页数量不限）
     *
     * @param queryRequest 查询请求
     * @return 脱敏后的应用分页数据
     */
    @Override
    public Page<AppVO> listAppVOByPageByAdmin(AppQueryRequest queryRequest) {
        Page<App> page = new Page<>(queryRequest.getPageNum(), queryRequest.getPageSize());

        QueryWrapper wrapper = new QueryWrapper();
        if (CharSequenceUtil.isNotBlank(queryRequest.getAppName())) {
            wrapper.like(App::getAppName, queryRequest.getAppName());
        }
        if (queryRequest.getUserId() != null) {
            wrapper.eq(App::getUserId, queryRequest.getUserId());
        }
        if (CharSequenceUtil.isNotBlank(queryRequest.getCodeGenType())) {
            wrapper.eq(App::getCodeGenType, queryRequest.getCodeGenType());
        }
        if (queryRequest.getPriority() != null) {
            wrapper.eq(App::getPriority, queryRequest.getPriority());
        }
        if (CharSequenceUtil.isNotBlank(queryRequest.getDeployKey())) {
            wrapper.eq(App::getDeployKey, queryRequest.getDeployKey());
        }
        wrapper.orderBy(App::getCreateTime, false);

        Page<App> appPage = this.page(page, wrapper);
        return convertToVOPage(appPage);
    }

    /**
     * 根据 id 获取应用（未脱敏，管理员用）
     *
     * @param id 应用 id
     * @return 应用完整信息
     */
    @Override
    public App getAppVOByIdByAdmin(Long id) {
        App app = this.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return app;
    }

    // endregion

    /**
     * 构建脱敏后的应用 VO
     *
     * @param app 应用实体
     * @return 脱敏后的应用 VO
     */
    private AppVO buildAppVO(App app) {
        AppVO vo = new AppVO();
        BeanUtil.copyProperties(app, vo);
        if (app.getUserId() != null) {
            try {
                vo.setUser(userService.getUserVOById(app.getUserId()));
            } catch (BusinessException ignored) {
                // User may have been deleted; keep app data readable.
            }
        }
        return vo;
    }

    /**
     * 将应用分页转换为 VO 分页
     *
     * @param appPage 应用分页
     * @return VO 分页
     */
    private Page<AppVO> convertToVOPage(Page<App> appPage) {
        List<AppVO> voList = appPage.getRecords().stream()
                .map(this::buildAppVO)
                .toList();
        Page<AppVO> voPage = new Page<>(appPage.getPageNumber(), appPage.getPageSize(), appPage.getTotalRow());
        voPage.setRecords(voList);
        return voPage;
    }

}
