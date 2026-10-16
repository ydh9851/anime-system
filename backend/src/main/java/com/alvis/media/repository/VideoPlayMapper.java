package com.alvis.media.repository;

import com.alvis.media.domain.VideoInfo;
import com.alvis.media.domain.VideoPlay;
import com.alvis.media.domain.other.KeyValue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface VideoPlayMapper extends MediaBaseMapper <VideoPlay> {
    int insert(VideoPlay record);

    int insertSelective(VideoPlay record);

    /**
     * 获取本月视频播放数量
     * @param filter
     * @return
     */
    Integer selectVideoPlayCount(VideoPlay filter);
    /**
     * 获取本月最佳影片
     * @param filter
     * @return
     */
   String selectBestVideo(VideoPlay filter);

   List<KeyValue> selectCountByDate(@Param("startTime") Date startTime, @Param("endTime") Date endTime);



   List<VideoPlay> selectVideoPlayInfo(VideoPlay filter);

    int updateByPrimaryKeySelective(VideoPlay record);

    /**
     * 按动漫 id 删除全部播放记录（删除动漫时先清子表，满足外键约束）
     *
     * @param videoId 动漫 id
     * @return 受影响行数
     */
    int deleteByVideoId(@Param("videoId") Integer videoId);
}