package com.xiaoyu.promptship.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xiaoyu.promptship.constant.AppConstant;
import com.xiaoyu.promptship.exception.BusinessException;
import com.xiaoyu.promptship.exception.ErrorCode;
import com.xiaoyu.promptship.exception.ThrowUtils;
import com.xiaoyu.promptship.mapper.AppMapper;
import com.xiaoyu.promptship.model.dto.AppCreateRequest;
import com.xiaoyu.promptship.model.dto.AppQueryRequest;
import com.xiaoyu.promptship.model.dto.AppUpdateMyRequest;
import com.xiaoyu.promptship.model.dto.AppUpdateRequest;
import com.xiaoyu.promptship.model.entity.App;
import com.xiaoyu.promptship.model.entity.User;
import com.xiaoyu.promptship.model.enums.CodeGenTypeEnum;
import com.xiaoyu.promptship.model.vo.AppVO;
import com.xiaoyu.promptship.service.AppService;
import com.xiaoyu.promptship.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用 服务层实现。
 *
 * @author xiaoyu
 * @since 1.0
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

    /**
     * 创建应用（用户用）
     *
     * @param request     创建请求
     * @param httpRequest HTTP 请求
     * @return 新应用 id
     */
    @Override
    public long createApp(AppCreateRequest request, HttpServletRequest httpRequest) {
        User currentUser = userService.getLoginUser(httpRequest);

        App app = new App();
        //暂时默认appName为提示词的前12位
        String appName = CharSequenceUtil.isNotBlank(request.getAppName())
                ? request.getAppName()
                : CharSequenceUtil.subPre(request.getInitPrompt(), 12);
        app.setAppName(appName);
        app.setInitPrompt(request.getInitPrompt());
        app.setUserId(currentUser.getId());
        // 暂时默认生成多文件
        app.setCodeGenType(CodeGenTypeEnum.MULTI_FILE.getValue());
        // 暂时默认优先级为普通
        app.setPriority(0);

        boolean saved = this.save(app);
        ThrowUtils.throwIf(!saved, new BusinessException(ErrorCode.SYSTEM_ERROR, "创建失败，请稍后重试"));

        return app.getId();
    }

    /**
     * 更新自己的应用（用户用，仅允许修改名称）
     *
     * @param request     更新请求
     * @param httpRequest HTTP 请求
     * @return 脱敏后的应用信息
     */
    @Override
    public AppVO updateApp(AppUpdateMyRequest request, HttpServletRequest httpRequest) {
        User currentUser = userService.getLoginUser(httpRequest);

        App app = this.getById(request.getId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!app.getUserId().equals(currentUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权修改该应用");

        if (CharSequenceUtil.isNotBlank(request.getAppName())) {
            app.setAppName(request.getAppName());
        }
        app.setEditTime(LocalDateTime.now());

        boolean updated = this.updateById(app);
        ThrowUtils.throwIf(!updated, new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败，请稍后重试"));

        return buildAppVO(app);
    }

    /**
     * 删除自己的应用（用户用）
     *
     * @param id          应用 id
     * @param httpRequest HTTP 请求
     * @return 是否成功
     */
    @Override
    public boolean deleteApp(Long id, HttpServletRequest httpRequest) {
        User currentUser = userService.getLoginUser(httpRequest);

        App app = this.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!app.getUserId().equals(currentUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权删除该应用");

        boolean removed = this.removeById(id);
        ThrowUtils.throwIf(!removed, new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败，请稍后重试"));

        return true;
    }

    /**
     * 根据 id 获取应用（脱敏）
     *
     * @param id 应用 id
     * @return 脱敏后的应用信息
     */
    @Override
    public AppVO getAppVOById(Long id) {
        App app = this.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return buildAppVO(app);
    }

    /**
     * 分页获取当前用户的应用列表（脱敏，支持根据名称查询，每页最多 20 个）
     *
     * @param queryRequest 查询请求
     * @param httpRequest  HTTP 请求
     * @return 脱敏后的应用分页数据
     */
    @Override
    public Page<AppVO> listMyAppVOByPage(AppQueryRequest queryRequest, HttpServletRequest httpRequest) {
        User currentUser = userService.getLoginUser(httpRequest);

        // 限制每页最大数量
        int pageSize = Math.min(queryRequest.getPageSize(), AppConstant.MAX_PAGE_SIZE);
        Page<App> page = new Page<>(queryRequest.getPageNum(), pageSize);

        QueryWrapper wrapper = new QueryWrapper()
                .eq(App::getUserId, currentUser.getId());
        if (CharSequenceUtil.isNotBlank(queryRequest.getAppName())) {
            wrapper.like(App::getAppName, queryRequest.getAppName());
        }
        wrapper.orderBy(App::getCreateTime, false);

        Page<App> appPage = this.page(page, wrapper);
        return convertToVOPage(appPage);
    }

    /**
     * 分页获取精选应用列表（脱敏，支持根据名称查询，每页最多 20 个）
     *
     * @param queryRequest 查询请求
     * @return 脱敏后的应用分页数据
     */
    @Override
    public Page<AppVO> listGoodAppVOByPage(AppQueryRequest queryRequest) {
        // 限制每页最大数量
        int pageSize = Math.min(queryRequest.getPageSize(), AppConstant.MAX_PAGE_SIZE);
        Page<App> page = new Page<>(queryRequest.getPageNum(), pageSize);

        QueryWrapper wrapper = new QueryWrapper()
                .ge(App::getPriority, AppConstant.FEATURED_MIN_PRIORITY);
        if (CharSequenceUtil.isNotBlank(queryRequest.getAppName())) {
            wrapper.like(App::getAppName, queryRequest.getAppName());
        }
        wrapper.orderBy(App::getPriority, false)
                .orderBy(App::getCreateTime, false);

        Page<App> appPage = this.page(page, wrapper);
        return convertToVOPage(appPage);
    }

    // region 管理员

    /**
     * 删除应用（管理员用）
     *
     * @param id 应用 id
     * @return 是否成功
     */
    @Override
    public boolean deleteAppByAdmin(Long id) {
        App app = this.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);

        boolean removed = this.removeById(id);
        ThrowUtils.throwIf(!removed, new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败，请稍后重试"));

        return true;
    }

    /**
     * 更新应用（管理员用，支持更新名称、封面、优先级）
     *
     * @param request 更新请求
     * @return 脱敏后的应用信息
     */
    @Override
    public AppVO updateAppByAdmin(AppUpdateRequest request) {
        App app = this.getById(request.getId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);

        if (CharSequenceUtil.isNotBlank(request.getAppName())) {
            app.setAppName(request.getAppName());
        }
        if (CharSequenceUtil.isNotBlank(request.getCover())) {
            app.setCover(request.getCover());
        }
        if (request.getPriority() != null) {
            app.setPriority(request.getPriority());
        }
        app.setEditTime(LocalDateTime.now());

        boolean updated = this.updateById(app);
        ThrowUtils.throwIf(!updated, new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败，请稍后重试"));

        return buildAppVO(app);
    }

    /**
     * 分页获取应用列表（管理员用，支持根据除时间外的字段查询，每页数量不限）
     *
     * @param queryRequest 查询请求
     * @return 脱敏后的应用分页数据
     */
    @Override
    public Page<AppVO> listAppVOByPageByAdmin(AppQueryRequest queryRequest) {
        Page<App> page = new Page<>(queryRequest.getPageNum(), queryRequest.getPageSize());

        QueryWrapper wrapper = new QueryWrapper();
        if (CharSequenceUtil.isNotBlank(queryRequest.getAppName())) {
            wrapper.like(App::getAppName, queryRequest.getAppName());
        }
        if (queryRequest.getUserId() != null) {
            wrapper.eq(App::getUserId, queryRequest.getUserId());
        }
        if (CharSequenceUtil.isNotBlank(queryRequest.getCodeGenType())) {
            wrapper.eq(App::getCodeGenType, queryRequest.getCodeGenType());
        }
        if (queryRequest.getPriority() != null) {
            wrapper.eq(App::getPriority, queryRequest.getPriority());
        }
        if (CharSequenceUtil.isNotBlank(queryRequest.getDeployKey())) {
            wrapper.eq(App::getDeployKey, queryRequest.getDeployKey());
        }
        wrapper.orderBy(App::getCreateTime, false);

        Page<App> appPage = this.page(page, wrapper);
        return convertToVOPage(appPage);
    }

    /**
     * 根据 id 获取应用（未脱敏，管理员用）
     *
     * @param id 应用 id
     * @return 应用完整信息
     */
    @Override
    public App getAppVOByIdByAdmin(Long id) {
        App app = this.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return app;
    }

    // endregion

    /**
     * 构建脱敏后的应用 VO
     *
     * @param app 应用实体
     * @return 脱敏后的应用 VO
     */
    private AppVO buildAppVO(App app) {
        AppVO vo = new AppVO();
        BeanUtil.copyProperties(app, vo);
        return vo;
    }

    /**
     * 将应用分页转换为 VO 分页
     *
     * @param appPage 应用分页
     * @return VO 分页
     */
    private Page<AppVO> convertToVOPage(Page<App> appPage) {
        List<AppVO> voList = appPage.getRecords().stream()
                .map(this::buildAppVO)
                .toList();
        Page<AppVO> voPage = new Page<>(appPage.getPageNumber(), appPage.getPageSize(), appPage.getTotalRow());
        voPage.setRecords(voList);
        return voPage;
    }

}
