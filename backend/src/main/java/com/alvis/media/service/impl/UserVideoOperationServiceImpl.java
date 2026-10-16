package com.alvis.media.service.impl;

import com.alvis.media.domain.UserVideoOperation;
import com.alvis.media.domain.UserVideoOperationKey;
import com.alvis.media.domain.VideoInfo;
import com.alvis.media.repository.UserVideoOperationMapper;
import com.alvis.media.repository.VideoInfoMapper;
import com.alvis.media.service.UserVideoOperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 奇趣
 */
@Service
public class UserVideoOperationServiceImpl implements UserVideoOperationService {

    private final UserVideoOperationMapper operationMapper;
    private final VideoInfoMapper videoInfoMapper;

    @Autowired
    public UserVideoOperationServiceImpl(UserVideoOperationMapper operationMapper,
                                         VideoInfoMapper videoInfoMapper) {
        this.operationMapper = operationMapper;
        this.videoInfoMapper = videoInfoMapper;
    }

    @Override
    public int toggleCollection(Integer userId, Integer videoId) {
        UserVideoOperationKey key = new UserVideoOperationKey();
        key.setId(userId);
        key.setVideoId(videoId);
        UserVideoOperation old = operationMapper.selectByPrimaryKey(key);
        if (old == null) {
            UserVideoOperation op = new UserVideoOperation();
            op.setId(userId);
            op.setVideoId(videoId);
            op.setCollection(1);
            op.setThumbUp(0);
            operationMapper.insertSelective(op);
            return 1;
        }
        int now = (old.getCollection() != null && old.getCollection() == 1) ? 0 : 1;
        old.setCollection(now);
        operationMapper.updateByPrimaryKeySelective(old);
        return now;
    }

    @Override
    public List<VideoInfo> listCollectionVideos(Integer userId) {
        List<Integer> ids = operationMapper.selectCollections(userId).stream()
                .map(UserVideoOperation::getVideoId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        return videoInfoMapper.selectByIds(ids);
    }

    @Override
    public int deleteByVideoId(Integer videoId) {
        return videoId == null ? 0 : operationMapper.deleteByVideoId(videoId);
    }
}