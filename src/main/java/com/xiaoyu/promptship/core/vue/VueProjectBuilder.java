package com.xiaoyu.promptship.core.vue;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.xiaoyu.promptship.constant.AppConstant;
import com.xiaoyu.promptship.exception.BusinessException;
import com.xiaoyu.promptship.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Vue 项目构建器。在 AI 完成文件生成后，执行 npm install 和 npm build。
 * 此步骤由代码固定驱动，不暴露给 AI。
 */
@Slf4j
@Component
public class VueProjectBuilder {

    private static final long TIMEOUT_SECONDS = 120;

    /**
     * 安装新增依赖并构建项目。
     *
     * @param appId         应用 id
     * @param dependencies  AI 声明的额外依赖列表（可为空）
     * @return 构建结果（成功信息或错误日志）
     */
    public BuildResult installAndBuild(Long appId, List<String> dependencies) {
        String projectDir = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_app_" + appId;
        File dir = new File(projectDir);
        if (!dir.exists()) {
            return BuildResult.fail("项目目录不存在: " + projectDir);
        }

        // 1. 追加 AI 声明的依赖到 package.json
        if (dependencies != null && !dependencies.isEmpty()) {
            appendDependencies(projectDir, dependencies);
        }

        // 2. npm install
        String installLog = runNpm(dir, "install");
        if (installLog.contains("npm ERR!")) {
            return BuildResult.fail("npm install 失败:\n" + installLog);
        }
        log.info("npm install 成功, appId={}", appId);

        // 3. npm run build
        String buildLog = runNpm(dir, "run", "build");
        if (buildLog.contains("npm ERR!") || buildLog.contains("error")) {
            return BuildResult.fail("npm run build 失败:\n" + buildLog);
        }
        log.info("npm run build 成功, appId={}", appId);

        return BuildResult.success("构建成功");
    }

    private void appendDependencies(String projectDir, List<String> dependencies) {
        File pkgFile = new File(projectDir, "package.json");
        String content = FileUtil.readString(pkgFile, StandardCharsets.UTF_8);

        for (String dep : dependencies) {
            String[] parts = dep.split("@", 2);
            String name = parts[0];
            String version = parts.length > 1 ? parts[1] : "^1.0.0";

            // 简单的 JSON 追加：在 "dependencies" 对象中插入
            String depEntry = "\n    \"" + name + "\": \"" + version + "\",";
            int insertPos = content.indexOf("\"dependencies\"");
            if (insertPos == -1) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "package.json 格式异常");
            }
            int braceStart = content.indexOf("{", insertPos);
            if (braceStart == -1) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "package.json 格式异常");
            }
            // 在 dependencies 的 { 之后插入
            content = content.substring(0, braceStart + 1) + depEntry + content.substring(braceStart + 1);
        }

        FileUtil.writeString(content, pkgFile, StandardCharsets.UTF_8);
        log.info("已追加依赖到 package.json: {}", dependencies);
    }

    private String runNpm(File workingDir, String... args) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(workingDir);
            // Windows 下使用 cmd /c npm，跨平台兼容
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                String[] cmd = new String[args.length + 2];
                cmd[0] = "cmd";
                cmd[1] = "/c";
                cmd[2] = "npm";
                System.arraycopy(args, 0, cmd, 3, args.length);
                pb.command(cmd);
            } else {
                String[] cmd = new String[args.length + 1];
                cmd[0] = "npm";
                System.arraycopy(args, 0, cmd, 1, args.length);
                pb.command(cmd);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "npm ERR! 执行超时（" + TIMEOUT_SECONDS + "秒）";
            }

            return output.toString();
        } catch (Exception e) {
            log.error("执行 npm 命令失败", e);
            return "npm ERR! " + e.getMessage();
        }
    }

    /**
     * 获取项目 dist 目录路径（部署时使用）。
     */
    public static String getDistPath(Long appId) {
        return AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_app_" + appId + "/dist";
    }

    public record BuildResult(boolean success, String message) {
        public static BuildResult success(String msg) {
            return new BuildResult(true, msg);
        }

        public static BuildResult fail(String msg) {
            return new BuildResult(false, msg);
        }
    }
}
