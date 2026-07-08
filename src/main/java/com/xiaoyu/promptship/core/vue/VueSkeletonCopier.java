package com.xiaoyu.promptship.core.vue;

import cn.hutool.core.io.FileUtil;
import com.xiaoyu.promptship.constant.AppConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Vue 骨架项目拷贝器。
 * 将 classpath 下 skeleton/vue/ 目录递归拷贝到 code_output/vue_app_{appId}/。
 */
@Slf4j
@Component
public class VueSkeletonCopier {

    private static final String SKELETON_PATH = "skeleton/vue/";

    /**
     * 将 Vue 骨架项目拷贝到指定 app 的工作目录。
     *
     * @param appId 应用 id
     * @return 项目根目录 File
     */
    public File copySkeleton(Long appId) throws IOException {
        String targetDir = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_app_" + appId;
        File target = new File(targetDir);
        FileUtil.mkdir(target);

        String pattern = "classpath:" + SKELETON_PATH + "**/*";
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(pattern);

        for (Resource resource : resources) {
            String urlPath = resource.getURL().getPath();
            if (!urlPath.contains(SKELETON_PATH)) {
                continue;
            }
            int idx = urlPath.indexOf(SKELETON_PATH);
            String relativePath = urlPath.substring(idx + SKELETON_PATH.length());
            if (relativePath.isEmpty()) {
                continue;
            }
            File destFile = new File(target, relativePath);
            // Resource.isFile() 仅判断协议是否为 file:（目录也返回 true），
            // 必须用 File.isFile() 判断是否为常规文件，否则目录会被当作文件读取
            File sourceFile = resource.getFile();
            if (sourceFile.isFile()) {
                FileUtil.mkParentDirs(destFile);
                String content = FileUtil.readString(sourceFile, StandardCharsets.UTF_8);
                FileUtil.writeString(content, destFile, StandardCharsets.UTF_8);
            }
        }

        log.info("Vue 骨架已拷贝到: {}", targetDir);
        return target;
    }
}
