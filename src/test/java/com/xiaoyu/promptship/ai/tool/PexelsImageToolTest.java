package com.xiaoyu.promptship.ai.tool;

import com.xiaoyu.promptship.ai.model.ImageResource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PexelsImageToolTest {

    @Value("${pexels.api-key}")
    private String pexelsApiKey;

    @Test
    void getPexelsImageTool() {
        PexelsImageTool pexelsTool = new PexelsImageTool(pexelsApiKey);
        List<ImageResource> resourceList = pexelsTool.searchContentImages("小狗", 5);
        int countNum =1;
        for (ImageResource image : resourceList) {
            System.out.println("==============================="+countNum+"===============================");
            System.out.println(image.getCategory());
            System.out.println(image.getDescription());
            System.out.println(image.getUrl());
            countNum++;
        }
    }

}