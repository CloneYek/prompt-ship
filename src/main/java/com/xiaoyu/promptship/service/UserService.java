package com.xiaoyu.promptship.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.xiaoyu.promptship.model.dto.UserCreateRequest;
import com.xiaoyu.promptship.model.dto.UserUpdateMyRequest;
import com.xiaoyu.promptship.model.dto.UserUpdateRequest;
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
     * @return 是否成功
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 创建用户（管理员用）
     *
     * @param request 创建请求
     * @return 新用户 id
     */
    long createUser(UserCreateRequest request);

    /**
     * 更新用户（管理员用）
     *
     * @param request 更新请求
     * @return 脱敏后的用户信息
     */
    LoginUserVO updateUser(UserUpdateRequest request);

    /**
     * 更新当前登录用户自己的信息
     *
     * @param request 更新请求（仅允许修改昵称、头像、简介）
     * @param httpRequest HTTP 请求
     * @return 脱敏后的用户信息
     */
    LoginUserVO updateMyUser(UserUpdateMyRequest request, HttpServletRequest httpRequest);

    /**
     * 根据 id 获取用户（脱敏）
     *
     * @param id 用户 id
     * @return 脱敏后的用户信息
     */
    LoginUserVO getUserVOById(long id);

    /**
     * 分页获取用户列表（脱敏）
     *
     * @param page 分页参数
     * @return 脱敏后的用户分页数据
     */
    Page<LoginUserVO> pageUsers(Page<User> page);
}
