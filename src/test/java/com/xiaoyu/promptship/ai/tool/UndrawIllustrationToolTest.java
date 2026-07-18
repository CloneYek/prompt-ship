package com.xiaoyu.promptship.ai.tool;

import com.xiaoyu.promptship.ai.model.ImageResource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class UndrawIllustrationToolTest {


    @Value("${undraw.build-id}")
    private String undrawBuildId;

    @Test
    void testUndrawIllustrationTool() {
        UndrawIllustrationTool undrawTool = new UndrawIllustrationTool(undrawBuildId);
        List<ImageResource> resourceList = undrawTool.searchIllustrations("happy");
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