package com.alvis.media.viewmodel.video;

import com.alvis.media.domain.VideoInfo;
import com.alvis.media.utility.DateTimeUtil;
import com.alvis.media.viewmodel.BaseVM;
import lombok.Data;

/**
 *@author 奇趣
 */

@Data
public class VideoResponseVM extends BaseVM {

    private Integer videoId;



    private String videoName;


    private Integer videoCategory;

    private String createTime;

    private Integer creatorId;

    private String lastModifyTime;

    private String videoUrl;

    private Long budget;

    private Long revenue;

    private Double popularity;

    private Double voteAverage;

    // ===== 详情页扩展字段 =====
    private String originalTitle;

    private String overview;

    private String tagline;

    private Integer runtime;

    private String originalLanguage;

    private String posterPath;

    private Double heatScore;

    private Integer voteCount;

    // 仅保留年份字符串，避免 Date -> String 映射问题
    private String releaseYear;

    public static VideoResponseVM from(VideoInfo videoInfo) {
        VideoResponseVM vm = modelMapper.map(videoInfo, VideoResponseVM.class);
        vm.setLastModifyTime(DateTimeUtil.dateFormat(videoInfo.getLastModifyTime()));
        vm.setCreateTime(DateTimeUtil.dateFormat(videoInfo.getCreateTime()));
        if (videoInfo.getReleaseDate() != null) {
            vm.setReleaseYear(String.valueOf(videoInfo.getReleaseDate().toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).getYear()));
        }
        return vm;
    }
}
