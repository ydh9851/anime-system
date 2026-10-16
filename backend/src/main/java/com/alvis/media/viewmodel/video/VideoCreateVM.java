package com.alvis.media.viewmodel.video;

import com.alvis.media.domain.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class VideoCreateVM {
    private Integer videoId;

    @NotBlank(message = "封面地址不能为空")
    private String videoUrl;

    @NotBlank(message = "动漫名字不能为空")
    private String videoName;

    @NotNull(message = "动漫分类不能为空")
    private Integer videoCategory;

    /** 标签列表（只取其中的 tagName），null 表示不带标签 */
    private List<Tag> videoTagList;
}
