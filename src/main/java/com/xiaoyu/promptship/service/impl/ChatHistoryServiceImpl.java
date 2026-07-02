package com.xiaoyu.promptship.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xiaoyu.promptship.common.CursorPage;
import com.xiaoyu.promptship.constant.AppConstant;
import com.xiaoyu.promptship.mapper.ChatHistoryMapper;
import com.xiaoyu.promptship.model.entity.ChatHistory;
import com.xiaoyu.promptship.model.enums.ChatHistoryRoleEnum;
import com.xiaoyu.promptship.model.vo.ChatHistoryVO;
import com.xiaoyu.promptship.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author xiaoyu
 * @since 1.0
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    /**
     * 从数据库加载历史对话到 ChatMemory。
     * 按时间倒序查询后反转，跳过最新一条（offset=1，即当前用户消息），
     * 确保 LangChain4j 自动追加当前消息时不会重复。
     *
     * @param appId      应用 id
     * @param chatMemory 目标记忆窗口
     * @param maxCount   最大加载条数
     * @return 实际加载的消息条数
     */
    @Override
    public int loadChatHistoryToMemory(Long appId, ChatMemory chatMemory, int maxCount) {
        try {
            // 按时间倒序查询，从第 2 条开始（offset=1），跳过最新的当前用户消息
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            // 反转列表，确保按时间正序（旧的在前，新的在后），正确的对话顺序
            historyList = CollUtil.reverse(historyList);

            // 先清空已有的记忆，防止重复加载
            chatMemory.clear();

            int loadedCount = 0;
            for (ChatHistory history : historyList) {
                if (ChatHistoryRoleEnum.USER.getValue().equals(history.getRole())) {
                    chatMemory.add(UserMessage.from(history.getContent()));
                    loadedCount++;
                } else if (ChatHistoryRoleEnum.ASSISTANT.getValue().equals(history.getRole())) {
                    chatMemory.add(AiMessage.from(history.getContent()));
                    loadedCount++;
                }
            }
            log.info("成功为 appId: {} 加载了 {} 条历史对话", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            // 加载失败不影响主流程，只是缺失历史上下文
            return 0;
        }
    }

    /**
     * 游标分页查询对话历史（按时间正序，老的在前）。
     * <p>
     * 使用 idx_appId_createTime 复合索引，通过 {@code createTime > cursor}
     * 直接定位到上一页末尾的位置继续查询，无需 OFFSET 跳过行，
     * 翻页深度不影响查询性能。
     * </p>
     * <p>
     * 多取一条（pageSize + 1）用于判断 hasMore，多的那条不返回给前端，
     * 但它的 createTime 用作返回给前端的 nextCursor。
     * </p>
     *
     * @param appId    应用 id
     * @param cursor   游标（上一页最后一条的 createTime ISO 字符串），首次传 null
     * @param pageSize 每页大小
     * @return 游标分页结果
     */
    @Override
    public CursorPage<ChatHistoryVO> listByAppIdCursor(Long appId, String cursor, int pageSize) {
        int limitSize = Math.min(pageSize, AppConstant.MAX_PAGE_SIZE);
        // 多取一条用于判断 hasMore
        int fetchSize = limitSize + 1;

        QueryWrapper wrapper = QueryWrapper.create()
                .eq(ChatHistory::getAppId, appId);

        // 游标条件：只查 createTime 大于游标值的数据
        if (cursor != null && !cursor.isEmpty()) {
            LocalDateTime cursorTime = LocalDateTime.parse(cursor, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            wrapper.gt(ChatHistory::getCreateTime, cursorTime);
        }

        wrapper.orderBy(ChatHistory::getCreateTime, true); // ASC，老的在前

        // MyBatis-Flex 的 list 直接取前 N 条，不走传统分页
        wrapper.limit(fetchSize);
        List<ChatHistory> records = this.list(wrapper);

        // 判断是否有下一页
        boolean hasMore = records.size() > limitSize;
        if (hasMore) {
            // 去掉多取的那一条
            records = records.subList(0, limitSize);
        }

        // 下一页游标 = 当前返回的最后一条记录的 createTime
        String nextCursor = null;
        if (!records.isEmpty()) {
            LocalDateTime lastCreateTime = records.get(records.size() - 1).getCreateTime();
            nextCursor = lastCreateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        List<ChatHistoryVO> voList = records.stream()
                .map(this::toVO)
                .toList();

        return CursorPage.of(voList, nextCursor, hasMore, limitSize);
    }

    /**
     * Entity 转 VO，使用 BeanUtil 复制属性
     */
    private ChatHistoryVO toVO(ChatHistory entity) {
        ChatHistoryVO vo = new ChatHistoryVO();
        BeanUtil.copyProperties(entity, vo);
        return vo;
    }

}
