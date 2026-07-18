package com.xiaoyu.promptship.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 图片类别枚举
 */
@Getter
public enum ImageCategoryEnum {

    CONTENT("内容图片", "CONTENT"),
    ILLUSTRATION("插画图片", "ILLUSTRATION"),
    LOGO("Logo图片", "LOGO"),
    ICON("图标", "ICON");

    private final String text;
    private final String value;

    ImageCategoryEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static ImageCategoryEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ImageCategoryEnum anEnum : ImageCategoryEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
