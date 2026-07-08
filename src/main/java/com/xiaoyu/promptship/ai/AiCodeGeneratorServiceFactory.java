package com.xiaoyu.promptship.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xiaoyu.promptship.ai.tool.DependencyTool;
import com.xiaoyu.promptship.ai.tool.FileTools;
import com.xiaoyu.promptship.constant.AppConstant;
import com.xiaoyu.promptship.service.ChatHistoryService;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI Service 工厂，管理 AI 服务的创建与缓存。
 * <p>
 * 每个 App 拥有独立的 {@link AiCodeGeneratorService} 实例，
 * 实例内部绑定独立的 {@link MessageWindowChatMemory}（滑动窗口记忆），
 * 实现不同应用之间的对话上下文隔离。
 * </p>
 * <p>
 * Caffeine 本地缓存策略：
 * <ul>
 *   <li>10 分钟内无访问则过期</li>
 *   <li>写入后 30 分钟强制过期</li>
 *   <li>最多缓存 200 个 App 的 Service 实例</li>
 * </ul>
 * </p>
 *
 * @author xiaoyu
 * @since 1.0
 */
@Configuration
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * Caffeine 缓存：appId → 绑定了独立 ChatMemory 的 AiCodeGeneratorService
     */
    private final Cache<Long, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .expireAfterWrite(Duration.ofMinutes(30))
            .maximumSize(200)
            .build();

    /**
     * Caffeine 缓存：appId → 绑定了独立 ChatMemory 和工具的 VueCodeGeneratorAgent
     */
    private final Cache<Long, VueCodeGeneratorAgent> vueAgentCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .expireAfterWrite(Duration.ofMinutes(30))
            .maximumSize(200)
            .build();

    /**
     * Caffeine 缓存：appId → DependencyTool 实例，用于在 AI 生成完成后读取声明的依赖列表
     */
    private final Cache<Long, DependencyTool> dependencyToolCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .expireAfterWrite(Duration.ofMinutes(30))
            .maximumSize(200)
            .build();

    /**
     * 默认的单例 Service（无 ChatMemory），供非对话场景使用（如初始生成）。
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return AiServices.builder(AiCodeGeneratorService.class)
                .streamingChatModel(streamingChatModel)
                .chatModel(chatModel)
                .build();
    }

    /**
     * 获取指定 App 的独立 AI Service。
     * <p>
     * 若缓存未命中，则创建新的 Service 实例：
     * <ol>
     *   <li>创建 {@link MessageWindowChatMemory}（滑动窗口，默认 20 条消息即 10 轮）</li>
     *   <li>从数据库加载历史对话到 ChatMemory</li>
     *   <li>构建绑定该 ChatMemory 的 AiCodeGeneratorService</li>
     *   <li>放入 Caffeine 缓存</li>
     * </ol>
     * </p>
     *
     * @param appId 应用 id
     * @return 绑定了该 App 对话记忆的 AI Service 实例
     */
    public AiCodeGeneratorService getServiceForApp(Long appId) {
        return serviceCache.get(appId, id -> {
            // 创建滑动窗口记忆，最多保留 10 轮对话
            ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(
                    AppConstant.CHAT_MEMORY_MAX_MESSAGES);
            // 从数据库加载历史对话到记忆
            chatHistoryService.loadChatHistoryToMemory(
                    appId, chatMemory, AppConstant.CHAT_MEMORY_MAX_MESSAGES);
            // 构建独立 AI Service
            return AiServices.builder(AiCodeGeneratorService.class)
                    .streamingChatModel(streamingChatModel)
                    .chatModel(chatModel)
                    .chatMemory(chatMemory)
                    .build();
        });
    }

    /**
     * 获取指定 App 的 Vue Agent（绑定工具的 AI 服务）。
     * <p>
     * 若缓存未命中，则创建新的 Agent 实例并装配工具：
     * <ol>
     *   <li>创建 {@link MessageWindowChatMemory}</li>
     *   <li>从数据库加载历史对话到 ChatMemory</li>
     *   <li>创建绑定到该 app 目录的 FileTools 和 DependencyTool</li>
     *   <li>构建 VueCodeGeneratorAgent 代理</li>
     * </ol>
     * </p>
     *
     * @param appId 应用 id
     * @return 绑定了工具和对话记忆的 Vue Agent 实例
     */
    public VueCodeGeneratorAgent getVueAgentForApp(Long appId) {
        return vueAgentCache.get(appId, id -> {
            ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(
                    AppConstant.CHAT_MEMORY_MAX_MESSAGES);
            chatHistoryService.loadChatHistoryToMemory(
                    appId, chatMemory, AppConstant.CHAT_MEMORY_MAX_MESSAGES);

            String basePath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_app_" + appId;
            FileTools fileTools = new FileTools(basePath);
            DependencyTool dependencyTool = new DependencyTool();
            dependencyToolCache.put(appId, dependencyTool);

            return AiServices.builder(VueCodeGeneratorAgent.class)
                    .streamingChatModel(streamingChatModel)
                    .chatMemory(chatMemory)
                    .tools(fileTools, dependencyTool)
                    .build();
        });
    }

    /**
     * 获取指定 App 的 DependencyTool，用于读取 AI 声明的依赖列表。
     * 调用前需确保已通过 {@link #getVueAgentForApp} 创建了 Agent。
     *
     * @param appId 应用 id
     * @return DependencyTool 实例（可能为空列表）
     */
    public DependencyTool getDependencyToolForApp(Long appId) {
        DependencyTool tool = dependencyToolCache.getIfPresent(appId);
        return tool != null ? tool : new DependencyTool();
    }

}
