package com.alvis.media.viewmodel.video;

import com.alvis.media.base.BasePage;
import lombok.Data;

import java.util.List;

/**
 * 动漫分页查询请求。
 *
 * <p>支持地区 / 语言 / 类型 / 题材 / 年份的多条件组合筛选：
 * 同一维度内多选为「或」，不同维度之间为「与」。
 *
 * @author 奇趣
 */
@Data
public class VideoPageRequestVM extends BasePage {

    /** 名称关键字，模糊匹配 */
    private String videoName;

    /** 地区，可多选，如 JP / CN / KR */
    private List<String> countries;

    /** 原始语言，可多选，如 ja / zh / ko */
    private List<String> languages;

    /** 动画类型，可多选，如 TV / Movie / OVA / ONA / Music / Special */
    private List<String> types;

    /** 题材（标签），可多选，如 Action / Comedy */
    private List<String> genres;

    /** 年份区间起（含），为空表示不限 */
    private Integer yearFrom;

    /** 年份区间止（含），为空表示不限 */
    private Integer yearTo;

    /** 最低评分，如 8 表示只看 8 分及以上 */
    private Double voteMin;

    /** 排序方式：heat / vote / release / default */
    private String orderBy;

    /** 只返回有封面的作品（过滤 CDN 已失效的封面） */
    private Boolean onlyWithPoster;
}
