package com.xiaoyu.promptship.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class AppServiceImplTest {

    // 测试AppName初始化方法

    @Test
    void getAppName() {
        String result = generateDefaultAppName("VibeCoding是什么意思？");
        System.out.println("result = " + result);
    }

    private String generateDefaultAppName(String initPrompt) {
        if (CharSequenceUtil.isBlank(initPrompt)) {
            return "未命名应用";
        }
        // 1. 取第一个分句（按常见分隔符切割）
        String first = initPrompt.split("[，,。！？\\n]")[0].trim();
        // 2. 去掉前缀动词/礼貌用语（长组合优先）
        first = first.replaceFirst(
                "^(请帮我做一个|请帮我生成一个|请帮我创建|帮我做一个|帮我生成一个|帮我创建|"
                        + "生成一个|创建一个|制作一个|做一个|写一个|"
                        + "帮我做|帮我|生成|创建|制作|写|请|帮忙|给我|介绍下|介绍)\\s*",
                "");
        first = first.trim();
        // 3. 疑问句式 → 陈述句式
        first = first.replaceAll("^(.+)是什么(意思)?$", "$1简介");
        first = first.replaceAll("^(.+)怎么做$", "$1制作指南");
        first = first.replaceAll("^(.+)如何(制作|创建|搭建)$", "$1$2指南");
        first = first.replaceAll("^(.+)和(.+)的区别(是什么)?$", "$1与$2对比");
        first = first.replaceAll("^(.+)和(.+)哪个好$", "$1与$2对比");
        // 4. 清洗后为空则回退到原始首句，仍为空则兜底
        if (first.isEmpty()) {
            first = initPrompt.split("[，,。！？\\n]")[0].trim();
        }
        if (first.isEmpty()) {
            return "未命名应用";
        }
        // 5. 截取合理长度
        return CharSequenceUtil.subPre(first, 18);
    }
}