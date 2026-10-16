package com.alvis.media.service;

import com.alvis.media.domain.VideoInfo;

import java.util.List;

/**
 * 用户收藏（t_user_video_operation）业务Service。
 * 原先 CollectionController 直接注入 UserVideoOperationMapper，抽出本接口后符合三层架构。
 */
public interface UserVideoOperationService {

    /**
     * 切换收藏状态（已收藏则取消，未收藏则收藏）
     *
     * @param userId  用户 id
     * @param videoId 动漫 id
     * @return 切换后的状态：1 已收藏，0 未收藏
     */
    int toggleCollection(Integer userId, Integer videoId);

    /**
     * 查询某用户收藏的动漫列表
     *
     * @param userId 用户 id
     * @return List<VideoInfo>
     */
    List<VideoInfo> listCollectionVideos(Integer userId);

    /**
     * 删除某动漫的全部收藏/评分记录（删除动漫时先清子表，满足外键约束）
     *
     * @param videoId 动漫 id
     * @return 受影响行数
     */
    int deleteByVideoId(Integer videoId);
}