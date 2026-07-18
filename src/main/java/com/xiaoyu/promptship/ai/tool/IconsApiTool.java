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
 * IconsApi 图标搜索工具
 */
@Slf4j
public class IconsApiTool {

    private static final String ICONS_API_URL = "https://iconsapi.com/api/search";

    private final String appKey;

    /** 本轮 AI 调用累积的图片结果 */
    private final List<ImageResource> collected = new ArrayList<>();

    public IconsApiTool(String appKey) {
        this.appKey = appKey;
    }

    @Tool("搜索图标，用于网站按钮、导航等UI元素")
    public List<ImageResource> searchIcons(@P("英文搜索关键词，如 home/settings/user") String query,
                                            @P("数量，默认8") int count) {
        List<ImageResource> results = new ArrayList<>();
        String apiUrl = String.format("%s?appkey=%s&query=%s", ICONS_API_URL, appKey, query);
        try (HttpResponse response = HttpRequest.get(apiUrl).timeout(10000).execute()) {
            if (!response.isOk()) {
                log.warn("iconsapi 异常: status={}", response.getStatus());
                return results;
            }
            JSONObject result = JSONUtil.parseObj(response.body());
            JSONObject pages = result.getJSONObject("pages");
            if (pages == null) {
                return results;
            }
            JSONArray elements = pages.getJSONArray("elements");
            if (elements == null || elements.isEmpty()) {
                return results;
            }
            int actualCount = Math.min(count, elements.size());
            for (int i = 0; i < actualCount; i++) {
                JSONObject item = elements.getJSONObject(i);
                String iconName = item.getStr("iconName", "图标");
                String url = item.getStr("url", "");
                if (StrUtil.isNotBlank(url)) {
                    results.add(ImageResource.builder()
                            .category(ImageCategoryEnum.ICON)
                            .description(iconName)
                            .url(url)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("iconsapi 搜索失败: query={}, error={}", query, e.getMessage());
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
