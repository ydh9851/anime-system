package com.alvis.media.service.impl;

import com.alvis.media.domain.Tag;
import com.alvis.media.domain.VideoTag;
import com.alvis.media.repository.TagMapper;
import com.alvis.media.repository.VideoTagMapper;
import com.alvis.media.service.VideoTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author 奇趣
 */
@Service
public class VideoTagServiceImpl implements VideoTagService {

    private final VideoTagMapper videoTagMapper;
    private final TagMapper tagMapper;

    @Autowired
    public VideoTagServiceImpl(VideoTagMapper videoTagMapper, TagMapper tagMapper) {
        this.videoTagMapper = videoTagMapper;
        this.tagMapper = tagMapper;
    }

    @Override
    public List<Tag> selectTagsByVideoId(Integer videoId) {
        if (videoId == null) {
            return List.of();
        }
        return videoTagMapper.selectTagsByVideoId(videoId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int replaceTags(Integer videoId, List<Tag> tagList) {
        if (videoId == null) {
            return 0;
        }
        // 先清空旧关联（覆盖语义）
        int affected = videoTagMapper.deleteByVideoId(videoId);
        if (CollectionUtils.isEmpty(tagList)) {
            return affected;
        }
        for (Tag tag : tagList) {
            if (tag == null || tag.getTagName() == null || tag.getTagName().trim().isEmpty()) {
                continue;
            }
            Integer tagId = resolveTagId(tag.getTagName().trim());
            VideoTag relation = new VideoTag();
            relation.setVideoId(videoId);
            relation.setTagId(tagId);
            videoTagMapper.insertSelective(relation);
            affected++;
        }
        return affected;
    }

    @Override
    public int deleteByVideoId(Integer videoId) {
        return videoId == null ? 0 : videoTagMapper.deleteByVideoId(videoId);
    }

    /**
     * 标签名 -> tagId，不存在则先落库再取 id（与 UserServiceImpl 中用户标签的处理保持一致）
     */
    private Integer resolveTagId(String tagName) {
        Tag filter = new Tag();
        filter.setTagName(tagName);
        Tag tagDb = tagMapper.selectTagByTagName(filter);
        if (tagDb == null) {
            tagMapper.insertSelective(filter);
            tagDb = tagMapper.selectTagByTagName(filter);
        }
        return tagDb == null ? null : tagDb.getTagId();
    }
}