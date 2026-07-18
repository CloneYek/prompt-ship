package com.xiaoyu.promptship.ai.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.tencentcloudapi.aiart.v20221229.AiartClient;
import com.tencentcloudapi.aiart.v20221229.models.QueryTextToImageJobRequest;
import com.tencentcloudapi.aiart.v20221229.models.QueryTextToImageJobResponse;
import com.tencentcloudapi.aiart.v20221229.models.SubmitTextToImageJobRequest;
import com.tencentcloudapi.aiart.v20221229.models.SubmitTextToImageJobResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.xiaoyu.promptship.ai.model.ImageResource;
import com.xiaoyu.promptship.manager.CosManager;
import com.xiaoyu.promptship.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 腾讯云混元 Logo 生成工具。
 * 异步提交生图任务 → 轮询等待完成 → 下载图片 → 上传 COS → 返回 COS URL。
 */
@Slf4j
public class LogoGeneratorTool {

    private static final String ENDPOINT = "aiart.tencentcloudapi.com";
    private static final String JOB_COMPLETED = "5";
    private static final String JOB_FAILED = "4";
    private static final long POLL_INTERVAL_MS = 2000;
    private static final long MAX_WAIT_MS = 10000;

    private final String secretId;
    private final String secretKey;
    private final String region;
    private final CosManager cosManager;
    private final AiartClient client;

    /** 本轮 AI 调用累积的图片结果 */
    private final List<ImageResource> collected = new ArrayList<>();

    public LogoGeneratorTool(String secretId, String secretKey, String region, CosManager cosManager) {
        this.secretId = secretId;
        this.secretKey = secretKey;
        this.region = region;
        this.cosManager = cosManager;
        this.client = buildClient();
    }

    private AiartClient buildClient() {
        Credential cred = new Credential(secretId, secretKey);
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint(ENDPOINT);
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        return new AiartClient(cred, region, clientProfile);
    }

    @Tool("根据描述生成 Logo 设计图片，用于网站品牌标识。注意：描述中禁止包含任何文字内容")
    public List<ImageResource> generateLogo(@P("Logo 设计描述，如名称、行业、风格，尽量详细") String description) {
        String logoPrompt = String.format(
                "生成 Logo图标。Logo 中严格禁止包含任何字母、汉字、数字、文字！"
                + "只需要图形符号。Logo 需求：%s", description);
        try {
            // 1. 提交生图任务
            SubmitTextToImageJobRequest submitReq = new SubmitTextToImageJobRequest();
            submitReq.setPrompt(logoPrompt);
            submitReq.setRevise(0L); // 关闭优化prompt
            submitReq.setLogoAdd(0L); // 关闭水印
            SubmitTextToImageJobResponse submitResp = client.SubmitTextToImageJob(submitReq);
            String jobId = submitResp.getJobId();
            log.info("Logo 生图任务已提交: jobId={}", jobId);

            // 2. 轮询等待完成
            String imageUrl = pollJob(jobId);
            if (StrUtil.isBlank(imageUrl)) {
                return Collections.emptyList();
            }

            // 3. 下载图片并上传 COS
            String cosUrl = downloadAndUploadToCos(imageUrl);
            if (StrUtil.isNotBlank(cosUrl)) {
                ImageResource resource = ImageResource.builder()
                        .category(ImageCategoryEnum.LOGO)
                        .description(description)
                        .url(cosUrl)
                        .build();
                collected.add(resource);
                return List.of(resource);
            }
        } catch (TencentCloudSDKException e) {
            log.error("Logo 生成失败: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * 轮询查询任务状态，直到完成或超时。
     */
    private String pollJob(String jobId) throws TencentCloudSDKException {
        long deadline = System.currentTimeMillis() + MAX_WAIT_MS;
        while (System.currentTimeMillis() < deadline) {
            QueryTextToImageJobRequest queryReq = new QueryTextToImageJobRequest();
            queryReq.setJobId(jobId);
            QueryTextToImageJobResponse queryResp = client.QueryTextToImageJob(queryReq);
            String status = queryResp.getJobStatusCode();

            if (JOB_COMPLETED.equals(status)) {
                String[] results = queryResp.getResultImage();
                if (results != null && results.length > 0) {
                    return results[0];
                }
                log.warn("Logo 生图完成但无结果图片: jobId={}", jobId);
                return null;
            }
            if (JOB_FAILED.equals(status)) {
                log.warn("Logo 生图失败: jobId={}, code={}, msg={}",
                        jobId, queryResp.getJobErrorCode(), queryResp.getJobErrorMsg());
                return null;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        log.warn("Logo 生图轮询超时: jobId={}", jobId);
        return null;
    }

    /**
     * 下载临时图片 URL 的文件，上传到 COS，返回永久 URL。
     */
    private String downloadAndUploadToCos(String tempImageUrl) {
        File tempFile = null;
        try {
            // 下载到本地临时文件
            String tmpDir = System.getProperty("java.io.tmpdir");
            tempFile = new File(tmpDir, "logo_" + UUID.randomUUID().toString().substring(0, 8) + ".png");
            HttpUtil.downloadFile(tempImageUrl, tempFile);

            // 上传到 COS
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String cosKey = String.format("/logos/%s/%s.png", datePath,
                    UUID.randomUUID().toString().substring(0, 8));
            return cosManager.uploadFile(cosKey, tempFile);
        } catch (Exception e) {
            log.error("Logo 上传 COS 失败: {}", e.getMessage());
            return null;
        } finally {
            FileUtil.del(tempFile);
        }
    }

    /** 取出并清空本轮累积的图片 */
    public List<ImageResource> drain() {
        List<ImageResource> copy = new ArrayList<>(collected);
        collected.clear();
        return copy;
    }
}
