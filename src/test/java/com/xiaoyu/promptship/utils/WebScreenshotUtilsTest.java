package com.xiaoyu.promptship.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class WebScreenshotUtilsTest {



    @Test
    void saveWebPageScreenshot() {
        String url = "https://www.baidu.com/";
        String webPageScreenshot = WebScreenshotUtils.saveWebPageScreenshot(url);
        Assertions.assertNotNull(webPageScreenshot);
    }
}