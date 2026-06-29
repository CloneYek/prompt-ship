package com.xiaoyu.promptship.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户创建请求（管理员用）
 *
 * @author xiaoyu
 * @since 1.0
 */
@Data
public class UserCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 账号
     */
    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 32, message = "账号长度需在 4~32 位之间")
    private String userAccount;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度需在 8~32 位之间")
    private String userPassword;

    /**
     * 用户昵称
     */
    @NotBlank(message = "用户昵称不能为空")
    @Size(max = 32, message = "用户昵称长度不能超过 32 位")
    private String userName;

    /**
     * 用户角色：user/admin，不传则默认为 user
     */
    private String userRole;

}
