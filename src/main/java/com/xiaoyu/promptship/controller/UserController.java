package com.xiaoyu.promptship.controller;

import com.mybatisflex.core.paginate.Page;
import com.xiaoyu.promptship.annotation.AuthCheck;
import com.xiaoyu.promptship.common.BaseResponse;
import com.xiaoyu.promptship.common.ResultUtils;
import com.xiaoyu.promptship.exception.ErrorCode;
import com.xiaoyu.promptship.exception.ThrowUtils;
import com.xiaoyu.promptship.model.dto.user.UserCreateRequest;
import com.xiaoyu.promptship.model.dto.user.UserLoginRequest;
import com.xiaoyu.promptship.model.dto.user.UserQueryRequest;
import com.xiaoyu.promptship.model.dto.user.UserRegisterRequest;
import com.xiaoyu.promptship.model.dto.user.UserUpdateMyRequest;
import com.xiaoyu.promptship.model.dto.user.UserUpdateRequest;
import com.xiaoyu.promptship.model.entity.User;
import com.xiaoyu.promptship.model.vo.LoginUserVO;
import com.xiaoyu.promptship.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户 控制层。
 *
 * @author xiaoyu
 * @since 1.0
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册。
     *
     * @param request 注册请求（账号、密码、确认密码）
     * @return 新用户 id
     */
    @PostMapping("/register")
    public BaseResponse<Long> register(@Valid @RequestBody UserRegisterRequest request) {
        long userId = userService.userRegister(
                request.getUserAccount(),
                request.getUserPassword(),
                request.getCheckPassword());
        return ResultUtils.success(userId);
    }

    /**
     * 用户登录。
     *
     * @param request 登录请求（账号、密码）
     * @return 脱敏后的登录用户信息
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> login(@Valid @RequestBody UserLoginRequest request,
                                           HttpServletRequest httpServletRequest) {
        LoginUserVO loginUserVO = userService.userLogin(
                request.getUserAccount(),
                request.getUserPassword(),
                httpServletRequest);
        return ResultUtils.success(loginUserVO);
    }


    /**
     * 根据 id 删除用户（管理员）。
     *
     * @param id 用户主键
     * @return 删除成功
     */
    @DeleteMapping("/remove/{id}")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> remove(@PathVariable Long id) {
        boolean result = userService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 获取当前登录用户。
     *
     * @param request HTTP 请求
     * @return 脱敏后的登录用户信息
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLogin(HttpServletRequest request) {
        LoginUserVO loginUserVO = userService.getLoginUserVO(request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 创建用户（管理员）。
     *
     * @param request 创建请求
     * @return 新用户 id
     */
    @PostMapping("/create")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Long> create(@Valid @RequestBody UserCreateRequest request) {
        long userId = userService.createUser(request);
        return ResultUtils.success(userId);
    }

    /**
     * 更新用户（管理员）。
     *
     * @param request 更新请求
     * @return 脱敏后的用户信息
     */
    @PutMapping("/update")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<LoginUserVO> update(@Valid @RequestBody UserUpdateRequest request) {
        LoginUserVO userVO = userService.updateUser(request);
        return ResultUtils.success(userVO);
    }

    /**
     * 更新当前登录用户自己的信息。
     *
     * @param request 更新请求（仅允许修改昵称、头像、简介）
     * @return 脱敏后的用户信息
     */
    @PutMapping("/update/my")
    @AuthCheck
    public BaseResponse<LoginUserVO> updateMy(@RequestBody UserUpdateMyRequest request,
                                               HttpServletRequest httpRequest) {
        LoginUserVO userVO = userService.updateMyUser(request, httpRequest);
        return ResultUtils.success(userVO);
    }

    /**
     * 分页获取用户列表（脱敏，管理员）。
     *
     * @param queryRequest 查询请求
     * @return 脱敏后的用户分页数据
     */
    @GetMapping("/page")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Page<LoginUserVO>> page(@ParameterObject UserQueryRequest queryRequest) {
        Page<LoginUserVO> userPage = userService.pageUsers(queryRequest);
        return ResultUtils.success(userPage);
    }

    /**
     * 根据 id 获取用户（未脱敏，管理员）。
     *
     * @param id 用户 id
     * @return 用户完整信息
     */
    @GetMapping("/get/{id}")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取用户（脱敏）。
     *
     * @param id 用户 id
     * @return 脱敏后的用户信息
     */
    @GetMapping("/get/vo/{id}")
    public BaseResponse<LoginUserVO> getVOById(@PathVariable Long id) {
        LoginUserVO userVO = userService.getUserVOById(id);
        return ResultUtils.success(userVO);
    }

    /**
     * 用户注销。
     *
     * @param request HTTP 请求
     * @return 成功响应
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }
}
