package com.xiaoyu.promptship.controller;

import com.xiaoyu.promptship.common.BaseResponse;
import com.xiaoyu.promptship.common.ResultUtils;
import com.xiaoyu.promptship.exception.ErrorCode;
import com.xiaoyu.promptship.exception.ThrowUtils;
import com.xiaoyu.promptship.model.dto.UserLoginRequest;
import com.xiaoyu.promptship.model.dto.UserRegisterRequest;
import com.xiaoyu.promptship.model.vo.LoginUserVO;
import com.xiaoyu.promptship.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
     * 用户删除。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return userService.removeById(id);
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
