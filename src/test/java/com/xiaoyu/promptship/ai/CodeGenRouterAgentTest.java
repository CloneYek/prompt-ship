package com.xiaoyu.promptship.ai;

import com.xiaoyu.promptship.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * AI 路由效果测试
 */
@SpringBootTest
class CodeGenRouterAgentTest {

    @Resource
    private CodeGenRouterAgent codeGenRouterAgent;

    @Test
    void routeSimplePage() {
        CodeGenTypeEnum result = codeGenRouterAgent.route("做一个简单的个人介绍页");
        assertNotNull(result);
        System.out.println("【简单展示页】路由结果: " + result.getValue() + " (" + result.getText() + ")");
    }

    @Test
    void routeMultiPage() {
        CodeGenTypeEnum result = codeGenRouterAgent.route("做一个公司首页，要包含首页、关于我们、联系我们三个页面");
        assertNotNull(result);
        System.out.println("【多页面网站】路由结果: " + result.getValue() + " (" + result.getText() + ")");
    }

    @Test
    void routeComplexApp() {
        CodeGenTypeEnum result = codeGenRouterAgent.route("做一个电商管理系统");
        assertNotNull(result);
        System.out.println("【复杂管理系统】路由结果: " + result.getValue() + " (" + result.getText() + ")");
    }
}
