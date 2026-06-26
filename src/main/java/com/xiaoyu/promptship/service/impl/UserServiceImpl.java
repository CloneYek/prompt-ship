package com.xiaoyu.promptship.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xiaoyu.promptship.exception.BusinessException;
import com.xiaoyu.promptship.exception.ErrorCode;
import com.xiaoyu.promptship.exception.ThrowUtils;
import com.xiaoyu.promptship.mapper.UserMapper;
import com.xiaoyu.promptship.model.entity.User;
import com.xiaoyu.promptship.model.enums.UserRoleEnum;
import com.xiaoyu.promptship.model.vo.LoginUserVO;
import com.xiaoyu.promptship.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

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
