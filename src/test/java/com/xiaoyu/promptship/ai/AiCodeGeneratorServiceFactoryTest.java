package com.xiaoyu.promptship.ai;

import com.xiaoyu.promptship.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorServiceFactoryTest {
    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult result = aiCodeGeneratorServiceFactory.aiCodeGeneratorService().generateMultiFileCode("生成一个美丽的“你好世界”页面");
        assertNotNull(result);
        System.out.println(result);
    }}