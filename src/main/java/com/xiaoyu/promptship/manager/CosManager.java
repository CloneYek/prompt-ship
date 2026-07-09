package com.xiaoyu.promptship.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.xiaoyu.promptship.config.CosConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * COS对象管理
 */
@Component
@Slf4j
public class CosManager {
    @Resource
    private CosConfig cosConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     * @param key 唯一键
     * @param file 文件
     * @return 上传结果
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传文件到COS并返回访问URL
     * @param key COS对象健（完整路径）
     * @param file 上传文件
     * @return 可访问的WebUrl或NULL
     */
    public String uploadFile(String key, File file) {
        //上传文件
        PutObjectResult result = putObject(key, file);
        if(result != null) {
            //构建访问URL
            String url =String.format("%s%s",cosConfig.getHost(),key);
            log.info("文件上传成功：{}->{}",file.getName(),url);
            return url;
        }
        else {
            log.error("文件上传失败,返回结果为空");
            return null;
        }
    }
}
