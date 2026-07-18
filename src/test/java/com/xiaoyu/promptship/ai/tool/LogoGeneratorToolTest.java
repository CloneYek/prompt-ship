package com.xiaoyu.promptship.ai.tool;

import com.xiaoyu.promptship.ai.model.ImageResource;
import com.xiaoyu.promptship.manager.CosManager;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class LogoGeneratorToolTest {

    @Resource
    private CosManager cosManager;

    @Value("${tencentcloud.secret-id}")
    private String tencentSecretId;

    @Value("${tencentcloud.secret-key}")
    private String tencentSecretKey;

    @Value("${tencentcloud.region}")
    private String tencentRegion;

    @Test
    void generateLogo() {
        LogoGeneratorTool logoTool = new LogoGeneratorTool(tencentSecretId, tencentSecretKey, tencentRegion, cosManager);
        List<ImageResource> resourceList = logoTool.generateLogo("生成一个宠物商店的Web页面的LOGO，风格应该为温暖治愈，LOGO名称为“PET”");
        resourceList.forEach(System.out::println);
    }
}