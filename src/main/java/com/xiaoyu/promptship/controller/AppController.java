package com.xiaoyu.promptship.controller;

import com.mybatisflex.core.paginate.Page;
import com.xiaoyu.promptship.annotation.AuthCheck;
import com.xiaoyu.promptship.common.BaseResponse;
import com.xiaoyu.promptship.common.ResultUtils;
import com.xiaoyu.promptship.exception.ErrorCode;
import com.xiaoyu.promptship.exception.ThrowUtils;
import com.xiaoyu.promptship.model.dto.AppChatContinueRequest;
import com.xiaoyu.promptship.model.dto.AppCreateRequest;
import com.xiaoyu.promptship.model.dto.AppDeployRequest;
import com.xiaoyu.promptship.model.dto.AppQueryRequest;
import com.xiaoyu.promptship.model.dto.AppUpdateMyRequest;
import com.xiaoyu.promptship.model.dto.AppUpdateRequest;
import com.xiaoyu.promptship.model.entity.App;
import com.xiaoyu.promptship.model.vo.AppVO;
import com.xiaoyu.promptship.service.AppService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * 应用 控制层。
 *
 * @author xiaoyu
 * @since 1.0
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Autowired
    private AppService appService;

    /**
     * 创建应用（用户）。
     *
     * @param request 创建请求（名称、初始化 prompt）
     * @return 新应用 id
     */
    @PostMapping("/create")
    @AuthCheck
    public BaseResponse<Long> createApp(@Valid @RequestBody AppCreateRequest request,
                                        HttpServletRequest httpRequest) {
        long appId = appService.createApp(request, httpRequest);
        return ResultUtils.success(appId);
    }

    /**
     * 创建应用并与 AI 流式对话生成代码（用户）。
     *
     * @param request 创建请求（提示词、应用名称）
     * @return SSE 流式响应（首个事件为应用元数据，后续为代码块）
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @AuthCheck
    public SseEmitter chatToGenCode(@Valid @RequestBody AppCreateRequest request,
                                    HttpServletRequest httpRequest) {
        SseEmitter emitter = new SseEmitter(600000L);

        Flux<String> flux = appService.chatToGenCode(request, httpRequest);
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
                    } catch (Exception e) {
                        // 连接已断开，忽略
                    }
                    emitter.complete();
                }
        );

        return emitter;
    }

    /**
     * 基于已有应用继续对话生成代码（用户，SSE 流式）。
     *
     * @param request 续聊请求（appId、新消息）
     * @return SSE 流式响应（首个事件为应用元数据，后续为代码块）
     */
    @PostMapping(value = "/chat/continue", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @AuthCheck
    public SseEmitter chatContinue(@Valid @RequestBody AppChatContinueRequest request,
                                   HttpServletRequest httpRequest) {
        SseEmitter emitter = new SseEmitter(600000L);

        Flux<String> flux = appService.chatContinue(request, httpRequest);
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
                    } catch (Exception e) {
                        // 连接已断开，忽略
                    }
                    emitter.complete();
                }
        );

        return emitter;
    }

    /**
     * 部署应用（用户）。将 code_output 下已生成的代码复制到 code_deploy，
     * 返回可公开访问的 URL。
     *
     * @param request 部署请求（appId）
     * @return 部署 URL，格式为 ${部署域名}/{deployKey}
     */
    @PostMapping("/deploy")
    @AuthCheck
    public BaseResponse<String> deployApp(@Valid @RequestBody AppDeployRequest request,
                                          HttpServletRequest httpRequest) {
        String deployUrl = appService.deployApp(request.getAppId(), httpRequest);
        return ResultUtils.success(deployUrl);
    }

    /**
     * 根据 id 修改自己的应用（用户）。
     *
     * @param request 更新请求（仅允许修改名称）
     * @return 脱敏后的应用信息
     */
    @PutMapping("/update")
    @AuthCheck
    public BaseResponse<AppVO> updateApp(@Valid @RequestBody AppUpdateMyRequest request,
                                         HttpServletRequest httpRequest) {
        AppVO appVO = appService.updateApp(request, httpRequest);
        return ResultUtils.success(appVO);
    }

    /**
     * 根据 id 删除自己的应用（用户）。
     *
     * @param id 应用主键
     * @return 删除成功
     */
    @DeleteMapping("/delete/{id}")
    @AuthCheck
    public BaseResponse<Boolean> deleteApp(@PathVariable Long id,
                                           HttpServletRequest httpRequest) {
        boolean result = appService.deleteApp(id, httpRequest);
        ThrowUtils.throwIf(!result, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 查看应用详情（用户）。
     *
     * @param id 应用 id
     * @return 脱敏后的应用信息
     */
    @GetMapping("/get/vo/{id}")
    @AuthCheck
    public BaseResponse<AppVO> getAppVOById(@PathVariable Long id) {
        AppVO appVO = appService.getAppVOById(id);
        return ResultUtils.success(appVO);
    }

    /**
     * 分页查询自己的应用列表（用户，支持根据名称查询，每页最多 20 个）。
     *
     * @param queryRequest 查询请求
     * @return 脱敏后的应用分页数据
     */
    @GetMapping("/list/my")
    @AuthCheck
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@ParameterObject AppQueryRequest queryRequest,
                                                        HttpServletRequest httpRequest) {
        Page<AppVO> appPage = appService.listMyAppVOByPage(queryRequest, httpRequest);
        return ResultUtils.success(appPage);
    }

    /**
     * 分页查询精选的应用列表（支持根据名称查询，每页最多 20 个）。
     *
     * @param queryRequest 查询请求
     * @return 脱敏后的应用分页数据
     */
    @GetMapping("/list/good")
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@ParameterObject AppQueryRequest queryRequest) {
        Page<AppVO> appPage = appService.listGoodAppVOByPage(queryRequest);
        return ResultUtils.success(appPage);
    }

    // region 管理员接口

    /**
     * 根据 id 删除任意应用（管理员）。
     *
     * @param id 应用主键
     * @return 删除成功
     */
    @DeleteMapping("/admin/delete/{id}")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> deleteAppByAdmin(@PathVariable Long id) {
        boolean result = appService.deleteAppByAdmin(id);
        ThrowUtils.throwIf(!result, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 更新任意应用（管理员，支持更新名称、封面、优先级）。
     *
     * @param request 更新请求
     * @return 脱敏后的应用信息
     */
    @PutMapping("/admin/update")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<AppVO> updateAppByAdmin(@Valid @RequestBody AppUpdateRequest request) {
        AppVO appVO = appService.updateAppByAdmin(request);
        return ResultUtils.success(appVO);
    }

    /**
     * 分页查询应用列表（管理员，支持根据除时间外的字段查询，每页数量不限）。
     *
     * @param queryRequest 查询请求
     * @return 脱敏后的应用分页数据
     */
    @GetMapping("/admin/list")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@ParameterObject AppQueryRequest queryRequest) {
        Page<AppVO> appPage = appService.listAppVOByPageByAdmin(queryRequest);
        return ResultUtils.success(appPage);
    }

    /**
     * 根据 id 查看应用详情（管理员，未脱敏）。
     *
     * @param id 应用 id
     * @return 应用完整信息
     */
    @GetMapping("/admin/get/{id}")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<App> getAppVOByIdByAdmin(@PathVariable Long id) {
        App app = appService.getAppVOByIdByAdmin(id);
        return ResultUtils.success(app);
    }

    // endregion
}
