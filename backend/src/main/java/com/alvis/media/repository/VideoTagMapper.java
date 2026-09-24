package com.alvis.media.repository;

import com.alvis.media.domain.Tag;
import com.alvis.media.domain.VideoTag;
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
}