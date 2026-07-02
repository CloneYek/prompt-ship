package com.xiaoyu.promptship.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 对话历史角色枚举
 *
 * @author xiaoyu
 * @since 1.0
 */
@Getter
public enum ChatHistoryRoleEnum {

    USER("用户消息", "user"),
    ASSISTANT("AI 回复", "assistant");

    private final String text;
    private final String value;

    ChatHistoryRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的 value
     * @return 枚举值，未匹配返回 null
     */
    public static ChatHistoryRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ChatHistoryRoleEnum anEnum : ChatHistoryRoleEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
