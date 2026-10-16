package com.alvis.media.service;

import com.alvis.media.domain.VideoInfo;
import com.alvis.media.viewmodel.video.VideoPageRequestVM;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 *@author 奇趣
 */
public interface VideoInfoService extends BaseService<VideoInfo> {
    /**
     * 带条件查询视频数量
     *
     * @param filter
     * @return
     */
    int selectNewVideoCount(VideoInfo filter);

    /**
     * 动漫库总数（全量，不是本月增量）
     *
     * @return t_video_info 记录总数
     */
    int countAll();

    /**
     * @param requestVM requestVM
     * @return PageInfo<VideoInfo>
     */
    PageInfo <VideoInfo> userPage(VideoPageRequestVM requestVM);

    List <VideoInfo> userAnalysis(Integer recommendUserId);

    int insertVideoPlay(String realUrl,Integer userId);

    int recalculateHeat();

    List <VideoInfo> heatTop(Integer limit);

    /**
     * 编辑动漫：更新基本信息并覆盖标签关联。
     * 整体一个事务，任一步失败全部回滚。
     *
     * @param record   含videoId 的动漫实体（仅非null 字段会被更新）
     * @param tagList  标签列表，为 null 表示不改动标签
     * @return 受影响行数
     */
    int updateVideo(VideoInfo record, List<com.alvis.media.domain.Tag> tagList);

    /**
     * 删除动漫：先清理播放记录 / 收藏评分 / 标签关联等子表，再删除主表记录。
     * t_video_info 的外键约束为 RESTRICT，顺序错误会直接抛 SQL 异常。
     *
     * @param videoId 动漫 id
     * @return true 删除成功；false 表示记录不存在
     */
    boolean deleteVideo(Integer videoId);

    /**
     * 按 id 批量查询动漫
     *
     * @param ids id 集合
     * @return List<VideoInfo>
     */
    List<VideoInfo> selectByIds(List<Integer> ids);

    /**
     * 按名称模糊查询动漫
     *
     * @param name  名称关键字
     * @param limit 返回条数上限，为空默认 20
     * @return List<VideoInfo>
     */
    List<VideoInfo> selectByNameLike(String name, Integer limit);

}