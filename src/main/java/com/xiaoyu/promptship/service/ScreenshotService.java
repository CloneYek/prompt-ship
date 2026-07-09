package com.xiaoyu.promptship.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 截图服务
 */
public interface ScreenshotService {

    /**
     * 通用的截图服务，可以获取访问url
     * @param webUrl webUrl地址
     * @return
     */
    public String generateAndUploadScreenshot(String webUrl);
}
