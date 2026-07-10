package com.xiaoyu.promptship.ai;

import com.xiaoyu.promptship.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.SystemMessage;

/**
 * 代码生成方案路由器。根据用户需求描述，返回最合适的代码生成类型。
 */
public interface CodeGenRouterAgent {

    @SystemMessage(fromResource = "template/system-prompt-router.txt")
    CodeGenTypeEnum route(String userMessage);
}
