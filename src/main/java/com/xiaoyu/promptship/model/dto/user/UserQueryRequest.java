package com.xiaoyu.promptship.model.dto.user;

import com.xiaoyu.promptship.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户查询请求
 *
 * @author xiaoyu
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户昵称（模糊查询）
     */
    private String userName;

    /**
     * 用户角色
     */
    private String userRole;

    /**
     * 用户账号（模糊查询）
     */
    private String userAccount;

}
