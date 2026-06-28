package com.xiaoyu.promptship.core;

import com.xiaoyu.promptship.ai.AiCodeGeneratorServiceFactory;
import com.xiaoyu.promptship.ai.model.HtmlCodeResult;
import com.xiaoyu.promptship.ai.model.MultiFileCodeResult;
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
 * Ai生成门面类，对外暴露唯一接口
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {
    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    /**
     * 统一生成入口：根据类型生成并保存代码
     * @param userMessage 用户提示词
     * @param codeGenTypeEnum 生成代码类型
     * @return
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        //参数校验
        ThrowUtils.throwIf(codeGenTypeEnum==null, ErrorCode.PARAMS_ERROR);
        return switch (codeGenTypeEnum){
            case HTML -> generateAndSaveHtmlCode(userMessage);
            case MULTI_FILE -> generateAndSaveMultiFileCode(userMessage);
            default -> {
                String message ="不支持生成的类型："+codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,message);
            }
        };
    }

    /**
     * 生成HTML模式的代码并保存
     * @param userMessage
     * @return
     */
    private File generateAndSaveHtmlCode(String userMessage) {
        HtmlCodeResult result = aiCodeGeneratorServiceFactory.aiCodeGeneratorService().generateHtmlCode(userMessage);
        return CodeFileSaver.saveHtmlCodeResult(result);
    }

    /**
     * 生成多文件模式的代码并保存
     * @param userMessage
     * @return
     */
    private File generateAndSaveMultiFileCode(String userMessage) {
        MultiFileCodeResult result = aiCodeGeneratorServiceFactory.aiCodeGeneratorService().generateMultiFileCode(userMessage);
        return CodeFileSaver.saveMultiFileCodeResult(result);
    }


    /**
     * 统一生成入口：根据类型生成并保存代码-流式
     * @param userMessage
     * @param codeGenTypeEnum
     * @return
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        //参数校验
        ThrowUtils.throwIf(codeGenTypeEnum==null, ErrorCode.PARAMS_ERROR);
        return switch (codeGenTypeEnum){
            case HTML -> generateAndSaveHtmlCodeStream(userMessage);
            case MULTI_FILE -> generateAndSaveMultiFileCodeStream(userMessage);
            default -> {
                String message ="不支持生成的类型："+codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,message);
            }
        };
    }

    /**
     * 流式生成多文件代码
     * @param userMessage
     * @return
     */
    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage) {
        Flux<String> resultFlux = aiCodeGeneratorServiceFactory.aiCodeGeneratorService().generateMultiFileCodeStream(userMessage);
        //流式返回生成代码完成后，再保存代码
        StringBuilder builder = new StringBuilder();
        return  resultFlux
                .doOnNext(chunk->{
                    //实时收集代码片段
                    builder.append(chunk);
                })
                .doOnComplete(()->{
                    try {
                        String completeMultiFileCode =builder.toString();
                        MultiFileCodeResult result = CodeParser.parseMultiFileCode(completeMultiFileCode);
                        //保存代码文件
                        File saveDir = CodeFileSaver.saveMultiFileCodeResult(result);
                        log.info("保存成功,路径为：{}", saveDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("保存失败", e);
                    }
                });
    }

    /**
     * 流式生成Html代码
     * @param userMessage
     * @return
     */
    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
        Flux<String> htmlCodeResultFlux = aiCodeGeneratorServiceFactory.aiCodeGeneratorService().generateHtmlCodeStream(userMessage);
        StringBuilder builder = new StringBuilder();
        return  htmlCodeResultFlux
                .doOnNext(chunk->{
                    builder.append(chunk);
                })
                .doOnComplete(()->{
                    try {
                        String completeHtmlCode =builder.toString();
                        HtmlCodeResult result = CodeParser.parseHtmlCode(completeHtmlCode);
                        File saveDir = CodeFileSaver.saveHtmlCodeResult(result);
                        log.info("文件保存成功:{}", saveDir.getAbsolutePath());
                    }catch (Exception e){
                        log.error("保存失败", e);
                    }
                });
    }
}
