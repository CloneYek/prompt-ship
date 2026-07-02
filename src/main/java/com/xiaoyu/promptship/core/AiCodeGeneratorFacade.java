package com.xiaoyu.promptship.core;

import com.xiaoyu.promptship.ai.AiCodeGeneratorService;
import com.xiaoyu.promptship.ai.AiCodeGeneratorServiceFactory;
import com.xiaoyu.promptship.core.parser.CodeParserExecutor;
import com.xiaoyu.promptship.core.saver.CodeFileSaverExecutor;
import com.xiaoyu.promptship.exception.BusinessException;
import com.xiaoyu.promptship.exception.ErrorCode;
import com.xiaoyu.promptship.exception.ThrowUtils;
import com.xiaoyu.promptship.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成门面，对外暴露统一入口。
 * <p>
 * 支持两种模式：
 * <ul>
 *   <li><b>无状态生成</b>（{@link #generateAndSaveCodeStream}）：使用默认单例 Service，不绑定对话记忆</li>
 *   <li><b>带记忆生成</b>（{@link #generateAndSaveCodeStreamForApp}）：使用 per-app Service，携带历史上下文</li>
 * </ul>
 * </p>
 *
 * @author yupi
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    /**
     * 统一生成入口：根据类型生成代码并保存（无状态，不携带历史上下文）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 代码生成类型
     * @param appId           应用 id
     * @return 保存目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) throws BusinessException {
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR);
        Object result = executeAiCall(userMessage, codeGenTypeEnum);
        return CodeFileSaverExecutor.executeSaver(result, codeGenTypeEnum, appId);
    }

    /**
     * 统一生成入口（流式，无状态）。
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 代码生成类型
     * @param appId           应用 id
     * @return 流式代码内容
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) throws BusinessException {
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR);
        Flux<String> flux = executeAiCallStream(userMessage, codeGenTypeEnum);
        return attachSaveOnComplete(flux, codeGenTypeEnum, appId);
    }

    /**
     * 统一生成入口（流式，携带该 App 的历史对话上下文）。
     * <p>
     * 使用 per-app AI Service（绑定独立 ChatMemory），
     * 确保 AI 能看到之前的对话内容。
     * </p>
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 代码生成类型
     * @param appId           应用 id
     * @return 流式代码内容
     */
    public Flux<String> generateAndSaveCodeStreamForApp(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) throws BusinessException {
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR);
        Flux<String> flux = executeAiCallStreamForApp(userMessage, codeGenTypeEnum, appId);
        return attachSaveOnComplete(flux, codeGenTypeEnum, appId);
    }

    /**
     * 调度非流式 AI 调用（使用默认单例 Service）
     */
    private Object executeAiCall(String userMessage, CodeGenTypeEnum type) {
        AiCodeGeneratorService service = aiCodeGeneratorServiceFactory.aiCodeGeneratorService();
        return switch (type) {
            case HTML -> service.generateHtmlCode(userMessage);
            case MULTI_FILE -> service.generateMultiFileCode(userMessage);
            default -> throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "不支持生成的类型：" + type.getValue());
        };
    }

    /**
     * 调度流式 AI 调用（使用默认单例 Service）
     */
    private Flux<String> executeAiCallStream(String userMessage, CodeGenTypeEnum type) {
        AiCodeGeneratorService service = aiCodeGeneratorServiceFactory.aiCodeGeneratorService();
        return switch (type) {
            case HTML -> service.generateHtmlCodeStream(userMessage);
            case MULTI_FILE -> service.generateMultiFileCodeStream(userMessage);
            default -> throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "不支持生成的类型：" + type.getValue());
        };
    }

    /**
     * 调度流式 AI 调用（使用 per-app Service，携带历史上下文）
     */
    private Flux<String> executeAiCallStreamForApp(String userMessage, CodeGenTypeEnum type, Long appId) {
        AiCodeGeneratorService service = aiCodeGeneratorServiceFactory.getServiceForApp(appId);
        return switch (type) {
            case HTML -> service.generateHtmlCodeStream(userMessage);
            case MULTI_FILE -> service.generateMultiFileCodeStream(userMessage);
            default -> throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "不支持生成的类型：" + type.getValue());
        };
    }

    /**
     * 为流式响应绑定保存逻辑：收集完整代码 → 解析 → 落盘
     *
     * @param flux 流式代码内容
     * @param type 代码生成类型
     * @param appId 应用 id
     * @return 透传原始流的 Flux
     */
    private Flux<String> attachSaveOnComplete(Flux<String> flux, CodeGenTypeEnum type, Long appId) {
        StringBuilder builder = new StringBuilder();
        return flux
                .doOnNext(builder::append)
                .doOnComplete(() -> {
                    try {
                        String completeCode = builder.toString();
                        Object result = CodeParserExecutor.executeParser(completeCode, type);
                        File saveDir = CodeFileSaverExecutor.executeSaver(result, type, appId);
                        log.info("代码保存成功，路径：{}", saveDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("代码保存失败", e);
                    }
                });
    }
}
