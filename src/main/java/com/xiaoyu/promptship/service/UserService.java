package com.xiaoyu.promptship.service;

import com.mybatisflex.core.service.IService;
import com.xiaoyu.promptship.model.entity.User;
import com.xiaoyu.promptship.model.vo.LoginUserVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户 服务层。
 *
 * @author xiaoyu
 * @since 1.0
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   账号
     * @param userPassword  密码
     * @param checkPassword 确认密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  账号
     * @param userPassword 密码
     * @param request      HTTP 请求
     * @return 脱敏后的登录用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request HTTP 请求
     * @return 当前登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户（脱敏后）
     *
     * @param request HTTP 请求
     * @return 脱敏后的登录用户信息
     */
    LoginUserVO getLoginUserVO(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request HTTP 请求
     */
    boolean userLogout(HttpServletRequest request);
}
