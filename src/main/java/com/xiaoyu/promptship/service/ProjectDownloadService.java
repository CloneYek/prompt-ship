package com.xiaoyu.promptship.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 项目下载
 */
public interface ProjectDownloadService {


    void downloadProjectZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
