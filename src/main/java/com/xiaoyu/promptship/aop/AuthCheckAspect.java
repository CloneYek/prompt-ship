package com.xiaoyu.promptship.aop;

import cn.hutool.core.text.CharSequenceUtil;
import com.xiaoyu.promptship.annotation.AuthCheck;
import com.xiaoyu.promptship.exception.BusinessException;
import com.xiaoyu.promptship.exception.ErrorCode;
import com.xiaoyu.promptship.model.entity.User;
import com.xiaoyu.promptship.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 权限校验 AOP 切面。
 *
 * @author xiaoyu
 * @since 1.0
 */
@Aspect
@Component
public class AuthCheckAspect {

    @Resource
    private UserService userService;

    /**
     * 拦截带有 @AuthCheck 注解的方法，校验当前用户是否具有指定角色。
     * <p>
     * mustRole 为空时，仅校验登录态；非空时，必须角色完全匹配才放行。
     *
     * @param joinPoint 切点
     * @param authCheck 权限校验注解
     * @return 被拦截方法的返回值
     * @throws Throwable 被拦截方法抛出的异常
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        //当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 校验是否具有指定角色
        if (CharSequenceUtil.isNotBlank(mustRole)) {
            String userRole = loginUser.getUserRole();
            if (!mustRole.equals(userRole)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }

        return joinPoint.proceed();
    }

}
