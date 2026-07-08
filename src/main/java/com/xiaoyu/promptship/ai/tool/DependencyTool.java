package com.xiaoyu.promptship.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * npm 依赖声明工具。AI 通过此工具声明需要安装的依赖包，
 * 实际安装由后端的固定流程统一执行（不在工具内直接执行 npm install）。
 */
@Slf4j
@Getter
public class DependencyTool {

    private final List<String> dependencies = new ArrayList<>();

    @Tool("声明需要安装的 npm 依赖包。vue 和 vue-router 已预装，无需重复添加")
    public String addDependency(@P("npm 包名") String name,
                                @P("版本号，如 ^1.0.0") String version) {
        if (name == null || name.isBlank()) {
            return "错误：包名不能为空";
        }
        String v = (version != null && !version.isBlank()) ? version : "latest";
        String dep = name + "@" + v;
        if (!dependencies.contains(dep)) {
            dependencies.add(dep);
            log.info("AI 声明依赖: {}", dep);
            return "已记录依赖: " + dep;
        }
        return "依赖已存在: " + dep;
    }
}
