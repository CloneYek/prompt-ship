package com.xiaoyu.promptship.ai.tool;

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
 * Pexels 图片搜索工具（内容图片）
 */
@Slf4j
public class PexelsImageTool {

    private static final String PEXELS_API_URL = "https://api.pexels.com/v1/search";

    private final String apiKey;

    /** 本轮 AI 调用累积的图片结果 */
    private final List<ImageResource> collected = new ArrayList<>();

    public PexelsImageTool(String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool("搜索内容图片素材，适用于产品图、场景图、人物图等")
    public List<ImageResource> searchContentImages(@P("英文搜索关键词") String query,
                                                    @P("数量，默认5") int count) {
        return doSearch(query, Math.min(count, 12), ImageCategoryEnum.CONTENT);
    }


    private List<ImageResource> doSearch(String query, int count, ImageCategoryEnum category) {
        List<ImageResource> results = new ArrayList<>();
        try (HttpResponse response = HttpRequest.get(PEXELS_API_URL)
                .header("Authorization", apiKey)
                .form("query", query)
                .form("per_page", count)
                .form("page", 1)
                .timeout(10000)
                .execute()) {
            if (!response.isOk()) {
                log.warn("Pexels API 异常: status={}", response.getStatus());
                return results;
            }
            JSONObject result = JSONUtil.parseObj(response.body());
            JSONArray photos = result.getJSONArray("photos");
            if (photos == null) {
                return results;
            }
            for (int i = 0; i < photos.size(); i++) {
                JSONObject photo = photos.getJSONObject(i);
                String url = photo.getJSONObject("src").getStr("medium", "");
                String alt = photo.getStr("alt", query);
                ImageResource resource = ImageResource.builder()
                        .category(category)
                        .description(alt)
                        .url(url)
                        .build();
                results.add(resource);
            }
        } catch (Exception e) {
            log.error("Pexels 搜索失败: query={}, error={}", query, e.getMessage());
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
