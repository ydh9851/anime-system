package com.alvis.media.service;

import com.alvis.media.domain.Tag;

import java.util.List;

/**
 * 动漫标签业务Service。
 * 原先 VideoController 直接注入 VideoTagMapper 调用，属于「Controller 跨层直连 Mapper」，
 * 抽出本接口后统一遵循 Controller -> Service -> Mapper 三层。
 */
public interface VideoTagService {

    /**
     * 查询某动漫绑定的标签
     *
     * @param videoId 动漫 id
     * @return List<Tag>
     */
    List<Tag> selectTagsByVideoId(Integer videoId);

    /**
     * 全量覆盖某动漫的标签关联（编辑动漫时调用）
     *
     * @param videoId  动漫 id
     * @param tagList  标签列表，为空表示清空标签
     * @return 受影响行数
     */
    int replaceTags(Integer videoId, List<Tag> tagList);

    /**
     * 删除某动漫的全部标签关联
     *
     * @param videoId 动漫 id
     * @return 受影响行数
     */
    int deleteByVideoId(Integer videoId);
}