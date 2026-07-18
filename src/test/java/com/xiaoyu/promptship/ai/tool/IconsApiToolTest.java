package com.xiaoyu.promptship.ai.tool;

import com.xiaoyu.promptship.ai.model.ImageResource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class IconsApiToolTest {
    @Value("${iconsapi.app-key}")
    private String iconsapiAppKey;

    @Test
    void getIconsApiKey() {
        IconsApiTool iconsTool = new IconsApiTool(iconsapiAppKey);
        List<ImageResource> resourceList = iconsTool.searchIcons("send", 4);
        resourceList.forEach(System.out::println);
    }
}