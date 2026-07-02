package com.xiaoyu.promptship.service;

import com.mybatisflex.core.service.IService;
import com.xiaoyu.promptship.common.CursorPage;
import com.xiaoyu.promptship.model.entity.ChatHistory;
import com.xiaoyu.promptship.model.vo.ChatHistoryVO;
import dev.langchain4j.memory.ChatMemory;

/**
 * 对话历史 服务层。
 *
 * @author xiaoyu
 * @since 1.0
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 从数据库加载历史对话到 ChatMemory。
     * 跳过最新一条消息（即当前请求本身），避免 LangChain4j 自动追加时重复。
     *
     * @param appId      应用 id
     * @param chatMemory 目标记忆窗口
     * @param maxCount   最大加载条数
     * @return 实际加载的消息条数
     */
    int loadChatHistoryToMemory(Long appId, ChatMemory chatMemory, int maxCount);

    /**
     * 游标分页查询对话历史（按时间正序，老的在前）。
     * <p>
     * 首次请求 cursor 传 null，返回第一页 + nextCursor。
     * 后续请求将 nextCursor 作为 cursor 传入以加载下一页。
     * </p>
     *
     * @param appId    应用 id
     * @param cursor   游标（上一页最后一条的 createTime），首次传 null
     * @param pageSize 每页大小
     * @return 游标分页结果
     */
    CursorPage<ChatHistoryVO> listByAppIdCursor(Long appId, String cursor, int pageSize);

}
