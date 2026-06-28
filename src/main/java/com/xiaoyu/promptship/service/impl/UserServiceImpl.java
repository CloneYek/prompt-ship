package com.xiaoyu.promptship.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xiaoyu.promptship.exception.BusinessException;
import com.xiaoyu.promptship.exception.ErrorCode;
import com.xiaoyu.promptship.exception.ThrowUtils;
import com.xiaoyu.promptship.mapper.UserMapper;
import com.xiaoyu.promptship.model.dto.UserCreateRequest;
import com.xiaoyu.promptship.model.dto.UserQueryRequest;
import com.xiaoyu.promptship.model.dto.UserUpdateMyRequest;
import com.xiaoyu.promptship.model.dto.UserUpdateRequest;
import com.xiaoyu.promptship.model.entity.User;
import com.xiaoyu.promptship.model.enums.UserRoleEnum;
import com.xiaoyu.promptship.model.vo.LoginUserVO;
import com.xiaoyu.promptship.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.xiaoyu.promptship.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 服务层实现。
 *
 * @author xiaoyu
 * @since 1.0
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /** 密码加密盐值 */
    private static final String SALT = "prompt-ship";

    /**
     * 用户注册
     * @param userAccount   账号
     * @param userPassword  密码
     * @param checkPassword 确认密码
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 校验两次密码是否一致
        ThrowUtils.throwIf(!userPassword.equals(checkPassword),
                ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");

        // 校验账号是否已存在
        long count = this.count(
                new QueryWrapper().eq(User::getUserAccount, userAccount));
        ThrowUtils.throwIf(count > 0,
                ErrorCode.PARAMS_ERROR, "该账号已被注册");

        // 密码加密
        String encryptedPassword = DigestUtil.md5Hex(SALT + userPassword);

        // 构建用户并保存
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptedPassword);
        user.setUserName(userAccount);
        user.setUserRole(UserRoleEnum.USER.getValue());

        boolean saved = this.save(user);
        ThrowUtils.throwIf(!saved, new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，请稍后重试"));

        return user.getId();
    }

    /**
     * 用户登录
     * @param userAccount  账号
     * @param userPassword 密码
     * @param request      HTTP 请求
     * @return
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 查询用户
        User user = this.getOne(
                new QueryWrapper().eq(User::getUserAccount, userAccount));
        ThrowUtils.throwIf(user == null,
                ErrorCode.PARAMS_ERROR, "账号或密码错误");

        // 校验密码
        String encryptedPassword = DigestUtil.md5Hex(SALT + userPassword);
        ThrowUtils.throwIf(!encryptedPassword.equals(user.getUserPassword()),
                ErrorCode.PARAMS_ERROR, "账号或密码错误");

        // 保存登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);

        return buildLoginUserVO(user);
    }

    /**
     * 获取用户登录
     * @param request HTTP 请求
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询最新状态
        long id = currentUser.getId();
        currentUser = this.getById(id);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public LoginUserVO getLoginUserVO(HttpServletRequest request) {
        return buildLoginUserVO(getLoginUser(request));
    }

    /**
     * 用户注销（移除登录态）
     * @param request HTTP 请求
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        //先校验用户是否登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        ThrowUtils.throwIf(userObj == null, ErrorCode.NOT_LOGIN_ERROR);
        //移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * 创建用户（管理员）
     * @param request 创建请求
     * @return
     */
    @Override
    public long createUser(UserCreateRequest request) {
        // 校验账号是否已存在
        long count = this.count(
                new QueryWrapper().eq(User::getUserAccount, request.getUserAccount()));
        ThrowUtils.throwIf(count > 0,
                ErrorCode.PARAMS_ERROR, "该账号已被注册");

        // 密码加密
        String encryptedPassword = DigestUtil.md5Hex(SALT + request.getUserPassword());

        // 构建用户
        User user = new User();
        user.setUserAccount(request.getUserAccount());
        user.setUserPassword(encryptedPassword);
        user.setUserName(request.getUserName());
        user.setUserRole(CharSequenceUtil.isNotBlank(request.getUserRole())
                ? request.getUserRole()
                : UserRoleEnum.USER.getValue());

        boolean saved = this.save(user);
        ThrowUtils.throwIf(!saved, new BusinessException(ErrorCode.SYSTEM_ERROR, "创建失败，请稍后重试"));

        return user.getId();
    }

    /**
     * 更新用户（管理员）
     * @param request 更新请求
     * @return
     */
    @Override
    public LoginUserVO updateUser(UserUpdateRequest request) {
        User user = this.getById(request.getId());
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);

        if (CharSequenceUtil.isNotBlank(request.getUserName())) {
            user.setUserName(request.getUserName());
        }
        if (CharSequenceUtil.isNotBlank(request.getUserAvatar())) {
            user.setUserAvatar(request.getUserAvatar());
        }
        if (CharSequenceUtil.isNotBlank(request.getUserProfile())) {
            user.setUserProfile(request.getUserProfile());
        }
        if (CharSequenceUtil.isNotBlank(request.getUserRole())) {
            user.setUserRole(request.getUserRole());
        }

        boolean updated = this.updateById(user);
        ThrowUtils.throwIf(!updated, new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败，请稍后重试"));

        return buildLoginUserVO(user);
    }

    /**
     * 获取脱敏用户信息
     * @param id 用户 id
     * @return
     */
    @Override
    public LoginUserVO getUserVOById(long id) {
        User user = this.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return buildLoginUserVO(user);
    }

    /**
     * 更新当前用户自己的信息
     * @param request 更新请求
     * @param httpRequest HTTP 请求
     * @return
     */
    @Override
    public LoginUserVO updateMyUser(UserUpdateMyRequest request, HttpServletRequest httpRequest) {
        User currentUser = getLoginUser(httpRequest);
        if (CharSequenceUtil.isNotBlank(request.getUserName())) {
            currentUser.setUserName(request.getUserName());
        }
        if (CharSequenceUtil.isNotBlank(request.getUserAvatar())) {
            currentUser.setUserAvatar(request.getUserAvatar());
        }
        if (CharSequenceUtil.isNotBlank(request.getUserProfile())) {
            currentUser.setUserProfile(request.getUserProfile());
        }
        boolean updated = this.updateById(currentUser);
        ThrowUtils.throwIf(!updated, new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败，请稍后重试"));
        return buildLoginUserVO(currentUser);
    }

    /**
     * 分页获取用户列表（脱敏）
     * @param queryRequest 查询请求
     * @return
     */
    @Override
    public Page<LoginUserVO> pageUsers(UserQueryRequest queryRequest) {
        Page<User> page = new Page<>(queryRequest.getPageNum(), queryRequest.getPageSize());

        QueryWrapper wrapper = new QueryWrapper();
        if (CharSequenceUtil.isNotBlank(queryRequest.getUserName())) {
            wrapper.like(User::getUserName, queryRequest.getUserName());
        }
        if (CharSequenceUtil.isNotBlank(queryRequest.getUserAccount())) {
            wrapper.like(User::getUserAccount, queryRequest.getUserAccount());
        }
        if (CharSequenceUtil.isNotBlank(queryRequest.getUserRole())) {
            wrapper.eq(User::getUserRole, queryRequest.getUserRole());
        }
        wrapper.orderBy(User::getCreateTime, false);

        Page<User> userPage = this.page(page, wrapper);
        List<LoginUserVO> voList = userPage.getRecords().stream()
                .map(this::buildLoginUserVO)
                .toList();
        Page<LoginUserVO> voPage = new Page<>(userPage.getPageNumber(), userPage.getPageSize(), userPage.getTotalRow());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 构建脱敏后的登录用户 VO
     *
     * @param user 用户实体
     * @return 脱敏后的登录用户 VO
     */
    private LoginUserVO buildLoginUserVO(User user) {
        LoginUserVO vo = new LoginUserVO();
        BeanUtil.copyProperties(user, vo);
        return vo;
    }

}
