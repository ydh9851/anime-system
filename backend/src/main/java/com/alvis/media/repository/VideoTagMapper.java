package com.alvis.media.repository;

import com.alvis.media.domain.Tag;
import com.alvis.media.domain.VideoTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VideoTagMapper {
    int insert(VideoTag record);

    int insertSelective(VideoTag record);

    @Select("SELECT t.tag_id AS tagId, t.tag_name AS tagName " +
            "FROM t_video_tag vt JOIN t_tag t ON vt.tag_id = t.tag_id " +
            "WHERE vt.video_id = #{videoId}")
    List<Tag> selectTagsByVideoId(@Param("videoId") Integer videoId);

    /**
     * 按动漫 id 删除标签关联（删除动漫时先清子表，满足外键约束）
     *
     * @param videoId 动漫 id
     * @return 受影响行数
     */
    @Delete("DELETE FROM t_video_tag WHERE video_id = #{videoId}")
    int deleteByVideoId(@Param("videoId") Integer videoId);
}