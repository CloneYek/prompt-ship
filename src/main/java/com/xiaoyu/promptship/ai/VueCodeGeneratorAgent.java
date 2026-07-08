package com.xiaoyu.promptship.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;

/**
 * Vue 工程化代码生成 AI Agent。
 * 返回 {@link TokenStream} 以支持流式工具调用——AI 可在生成过程中
 * 通过 write_file、add_dependency 等工具逐步构建项目。
 */
public interface VueCodeGeneratorAgent {

    @SystemMessage(fromResource = "template/system-prompt-vue.txt")
    TokenStream generate(String userMessage);
}
