package com.xiaoyu.promptship.ai;

import dev.langchain4j.service.SystemMessage;

/**
 * 图片收集 Agent。根据用户需求，通过 Tool Calling 自主搜索各类图片素材。
 * 返回 String 类型避免 DeepSeek JSON schema 兼容问题，图片数据从 Tool 的 drain() 获取。
 */
public interface ImageCollectionAgent {

    @SystemMessage(fromResource = "template/system-prompt-image-collection.txt")
    String collectImages(String userPrompt);
}
