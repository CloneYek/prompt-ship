package com.xiaoyu.promptship.exception;

import com.xiaoyu.promptship.common.BaseResponse;
import com.xiaoyu.promptship.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.StringJoiner;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 参数校验异常处理
     *
     * @param e 参数校验异常
     * @return 统一响应，message 中包含所有字段的错误信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        StringJoiner joiner = new StringJoiner("；");
        e.getBindingResult().getFieldErrors().forEach(error ->
                joiner.add(error.getDefaultMessage()));
        String errorMsg = joiner.toString();
        log.error("参数校验失败：{}", errorMsg);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, errorMsg);
    }

    /**
     * 业务异常处理
     *
     * @param e 业务异常
     * @return 统一响应
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * 运行时异常处理
     *
     * @param e 运行时异常
     * @return 统一响应
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}
