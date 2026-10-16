package com.alvis.media.viewmodel.admin.dashboard;

import com.alvis.media.domain.VideoInfo;
import com.alvis.media.domain.other.KeyValue;
import lombok.Data;

import java.util.List;

/**
 * 首页看板「可视化分布」数据：
 * 前端据此渲染柱状图（年份分布）、饼图（类型分布 / 语言分布）与 TOP 榜柱状图。
 */
@Data
public class DistributionVM {

    /** 发行年份分布 -> 柱状图 */
    private List<KeyValue> yearDistribution;

    /** 动画类型分布（TV/Movie/OVA/ONA/Special/Music）-> 饼图 */
    private List<KeyValue> typeDistribution;

    /** 题材标签分布 -> 饼图 */
    private List<KeyValue> genreDistribution;

    /** 原始语言分布 -> 饼图 */
    private List<KeyValue> languageDistribution;

    /** 热度 TOP10 -> 横向柱状图 */
    private List<VideoInfo> topRevenue;
}
