package com.xiaoyu.promptship.ai;

import com.xiaoyu.promptship.ai.model.HtmlCodeResult;
import com.xiaoyu.promptship.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {
    /***
     * 生成Html文件
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "template/system-prompt-html.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    /**
     * 生成多文件
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "template/system-prompt-multi.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);

    /**
     * 生成Html文件 -流式
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "template/system-prompt-html.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);


    /**
     * 生成多文件-流式
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "template/system-prompt-multi.txt")
    Flux<String> generateMultiFileCodeStream(String userMessage);
}
