package com.alvis.media.viewmodel.video;

import com.alvis.media.domain.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 编辑动漫入参。
 * 与 {@link VideoCreateVM} 分开：编辑时 videoId 由 URL 路径变量提供，
 * 且 videoUrl 允许为空（未换封面时保持原值）。
 */
@Data
public class VideoUpdateVM {

    @NotBlank(message = "动漫名字不能为空")
    private String videoName;

    @NotNull(message = "动漫分类不能为空")
    private Integer videoCategory;

    /** 封面/视频地址，为空表示不修改 */
    private String videoUrl;

    /** 标签列表：null 表示不改动标签；空数组表示清空标签 */
    private List<Tag> videoTagList;
}