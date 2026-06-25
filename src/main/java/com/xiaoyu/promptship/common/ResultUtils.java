package com.xiaoyu.promptship.common;

import com.xiaoyu.promptship.exception.ErrorCode;

/**
 * 快速构造响应结果的工具类
 */
public class ResultUtils {

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 统一响应
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    /**
     * 失败响应
     *
     * @param errorCode 错误码
     * @return 统一响应
     */
    public static BaseResponse<?> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败响应
     *
     * @param code    错误码
     * @param message 错误信息
     * @return 统一响应
     */
    public static BaseResponse<?> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    /**
     * 失败响应
     *
     * @param errorCode 错误码
     * @param message   错误信息
     * @return 统一响应
     */
    public static BaseResponse<?> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }
}
