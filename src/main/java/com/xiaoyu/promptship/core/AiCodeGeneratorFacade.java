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
 * AI 代码生成门面，对外暴露统一入口
 *
 * @author yupi
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    /**
     * 统一生成入口：根据类型生成代码并保存
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 代码生成类型
     * @return 保存目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum,Long appId) throws BusinessException {
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR);
        Object result = executeAiCall(userMessage, codeGenTypeEnum);
        return CodeFileSaverExecutor.executeSaver(result, codeGenTypeEnum, appId);
    }

    /**
     * 统一生成入口（流式）：根据类型流式生成代码并保存
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 代码生成类型
     * @return 流式代码内容
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) throws BusinessException {
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR);
        Flux<String> flux = executeAiCallStream(userMessage, codeGenTypeEnum);
        return attachSaveOnComplete(flux, codeGenTypeEnum, appId);
    }

    /**
     * 调度非流式 AI 调用
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
     * 调度流式 AI 调用
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
     * 为流式响应绑定保存逻辑：收集完整代码 → 解析 → 落盘
     *
     * @param flux 流式代码内容
     * @param type 代码生成类型
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
                        File saveDir = CodeFileSaverExecutor.executeSaver(result, type,appId);
                        log.info("代码保存成功，路径：{}", saveDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("代码保存失败", e);
                    }
                });
    }
}
