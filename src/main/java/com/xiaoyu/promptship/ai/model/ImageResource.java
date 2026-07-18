package com.xiaoyu.promptship.ai.model;

import com.xiaoyu.promptship.model.enums.ImageCategoryEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图片资源对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResource implements Serializable {

    /**
     * 图片类别
     */
    private ImageCategoryEnum category;

    /**
     * 图片描述（用于增强 prompt）
     */
    private String description;

    /**
     * 图片地址
     */
    private String url;

    @Serial
    private static final long serialVersionUID = 1L;
}
