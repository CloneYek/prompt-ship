package com.xiaoyu.promptship.controller;

import com.xiaoyu.promptship.annotation.AuthCheck;
import com.xiaoyu.promptship.common.BaseResponse;
import com.xiaoyu.promptship.common.CursorPage;
import com.xiaoyu.promptship.common.ResultUtils;
import com.xiaoyu.promptship.exception.BusinessException;
import com.xiaoyu.promptship.exception.ErrorCode;
import com.xiaoyu.promptship.exception.ThrowUtils;
import com.xiaoyu.promptship.model.dto.ChatHistoryQueryRequest;
import com.xiaoyu.promptship.model.entity.App;
import com.xiaoyu.promptship.model.entity.User;
import com.xiaoyu.promptship.model.vo.ChatHistoryVO;
import com.xiaoyu.promptship.service.AppService;
import com.xiaoyu.promptship.service.ChatHistoryService;
import com.xiaoyu.promptship.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对话历史 控制层。
 *
 * @author xiaoyu
 * @since 1.0
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    /**
     * 游标分页查询某应用下的对话历史（按时间正序，老的在前）。
     * 仅应用创建者可以查看对话历史。
     * <p>
     * 首次请求传 appId + pageSize，返回第一页数据 + nextCursor。
     * 后续请求将 nextCursor 作为 cursor 参数传入以加载下一页。
     * </p>
     *
     * @param queryRequest 查询请求（appId、cursor、pageSize）
     * @return 游标分页结果（records + nextCursor + hasMore）
     */
    @GetMapping("/list")
    @AuthCheck
    public BaseResponse<CursorPage<ChatHistoryVO>> listChatHistory(
            @Valid @ParameterObject ChatHistoryQueryRequest queryRequest,
            HttpServletRequest httpRequest) {
        // 校验该应用属于当前登录用户，防止越权查看他人对话记录
        App app = appService.getById(queryRequest.getAppId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        User currentUser = userService.getLoginUser(httpRequest);
        ThrowUtils.throwIf(!app.getUserId().equals(currentUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");

        CursorPage<ChatHistoryVO> page = chatHistoryService.listByAppIdCursor(
                queryRequest.getAppId(),
                queryRequest.getCursor(),
                queryRequest.getPageSize());
        return ResultUtils.success(page);
    }

}
