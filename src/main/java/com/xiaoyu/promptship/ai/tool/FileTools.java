package com.xiaoyu.promptship.ai.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Vue 项目文件操作工具集，供 AI Agent 调用。
 * 所有操作限定在 projectBasePath 目录下，防止路径穿越。
 */
@Slf4j
public class FileTools {

    private final String projectBasePath;

    public FileTools(String projectBasePath) {
        this.projectBasePath = projectBasePath;
    }

    @Tool("创建或覆盖项目文件。path 为相对于项目根目录的文件路径，content 为完整的文件内容")
    public String writeFile(@P("文件路径，如 src/App.vue") String path,
                            @P("完整的文件内容") String content) {
        if (StrUtil.isBlank(path) || content == null) {
            return "错误：文件路径和内容不能为空";
        }
        File file = resolveFile(path);
        FileUtil.mkParentDirs(file);
        FileUtil.writeString(content, file, StandardCharsets.UTF_8);
        log.info("writeFile: {}", path);
        return "文件已创建: " + path;
    }

    @Tool("读取项目文件内容")
    public String readFile(@P("文件路径，如 src/App.vue") String path) {
        if (StrUtil.isBlank(path)) {
            return "错误：文件路径不能为空";
        }
        File file = resolveFile(path);
        if (!file.exists()) {
            return "错误：文件不存在: " + path;
        }
        return FileUtil.readString(file, StandardCharsets.UTF_8);
    }

    @Tool("列出当前项目的完整文件结构")
    public String listFiles() {
        File base = new File(projectBasePath);
        if (!base.exists()) {
            return "项目目录为空";
        }
        return listTree(base, base);
    }

    @Tool("删除项目文件")
    public String deleteFile(@P("文件路径，如 src/components/Old.vue") String path) {
        if (StrUtil.isBlank(path)) {
            return "错误：文件路径不能为空";
        }
        File file = resolveFile(path);
        if (!file.exists()) {
            return "错误：文件不存在: " + path;
        }
        FileUtil.del(file);
        log.info("deleteFile: {}", path);
        return "文件已删除: " + path;
    }

    private File resolveFile(String path) {
        // 规范化路径，防止 ../ 路径穿越攻击
        String normalized = path.replace('\\', '/');
        File resolved = new File(projectBasePath, normalized);
        String canonicalBase = FileUtil.getAbsolutePath(new File(projectBasePath));
        String canonicalTarget = FileUtil.getAbsolutePath(resolved);
        if (!canonicalTarget.startsWith(canonicalBase)) {
            throw new SecurityException("不允许访问项目目录之外的文件: " + path);
        }
        return resolved;
    }

    private String listTree(File root, File dir) {
        File[] files = dir.listFiles();
        if (files == null) return "";
        StringBuilder sb = new StringBuilder();
        for (File f : files) {
            if ("node_modules".equals(f.getName()) || ".git".equals(f.getName())) {
                continue;
            }
            String relativePath = f.getAbsolutePath()
                    .substring(root.getAbsolutePath().length())
                    .replace('\\', '/');
            if (f.isDirectory()) {
                sb.append(relativePath).append("/\n");
                sb.append(listTree(root, f));
            } else {
                sb.append(relativePath).append("\n");
            }
        }
        return sb.toString();
    }
}
