package com.xiaoyu.promptship.ai.tool;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xiaoyu.promptship.ai.model.ImageResource;
import com.xiaoyu.promptship.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * undraw.co 插画搜索工具
 */
@Slf4j
public class UndrawIllustrationTool {

    private static final String UNDRAW_BASE_URL = "https://undraw.co/_next/data/%s/search/%s.json?term=%s";

    private final String buildId;

    /** 本轮 AI 调用累积的图片结果 */
    private final List<ImageResource> collected = new ArrayList<>();

    public UndrawIllustrationTool(String buildId) {
        this.buildId = buildId;
    }

    @Tool("搜索插画图片，用于网站美化和装饰")
    public List<ImageResource> searchIllustrations(@P("英文搜索关键词，如 happy/team/work") String query) {
        List<ImageResource> results = new ArrayList<>();
        String apiUrl = String.format(UNDRAW_BASE_URL, buildId, query, query);
        try (HttpResponse response = HttpRequest.get(apiUrl).timeout(10000).execute()) {
            if (!response.isOk()) {
                log.warn("undraw API 异常: status={}", response.getStatus());
                return results;
            }
            JSONObject result = JSONUtil.parseObj(response.body());
            JSONObject pageProps = result.getJSONObject("pageProps");
            if (pageProps == null) {
                return results;
            }
            JSONArray initialResults = pageProps.getJSONArray("initialResults");
            if (initialResults == null || initialResults.isEmpty()) {
                return results;
            }
            int count = Math.min(6, initialResults.size());
            for (int i = 0; i < count; i++) {
                JSONObject item = initialResults.getJSONObject(i);
                String title = item.getStr("title", "插画");
                String media = item.getStr("media", "");
                if (StrUtil.isNotBlank(media)) {
                    results.add(ImageResource.builder()
                            .category(ImageCategoryEnum.ILLUSTRATION)
                            .description(title)
                            .url(media)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("undraw 搜索失败: query={}, error={}", query, e.getMessage());
        }
        collected.addAll(results);
        return results;
    }

    /** 取出并清空本轮累积的图片 */
    public List<ImageResource> drain() {
        List<ImageResource> copy = new ArrayList<>(collected);
        collected.clear();
        return copy;
    }
}
