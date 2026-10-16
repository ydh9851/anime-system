package com.alvis.media.viewmodel.admin.dashboard;

import com.alvis.media.domain.VideoInfo;
import lombok.Data;

import java.util.List;

@Data
public class IndexVM {
    /** 本月新增注册用户数 */
    private Integer newUserCount;

    /** 动漫库总数（全量导入，不是本月增量） */
    private Integer videoTotal;

    /** 本月播放总次数 */
    private Integer doPlayVideoCount;

    /** 本月播放最热番剧名称 */
    private String hotVideoCount;

    private List<Integer> mothDayUserActionValue;
    private List<Integer> mothDayVideoPlayValue;
    private List<String> mothDayText;
    private List<VideoInfo> heatTopList;
}
