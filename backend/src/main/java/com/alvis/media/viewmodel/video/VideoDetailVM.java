package com.alvis.media.viewmodel.video;

import com.alvis.media.domain.Tag;
import lombok.Data;

import java.util.List;

@Data
public class VideoDetailVM {
    private Integer videoId;
    private String videoName;
    private Integer videoCategory;
    private String videoUrl;
    private List<Tag> videoTagList;
}
