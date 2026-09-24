package com.alvis.media.controller.admin;

import com.alvis.media.base.BaseApiController;
import com.alvis.media.base.RestResponse;
import com.alvis.media.domain.UserVideoOperation;
import com.alvis.media.domain.UserVideoOperationKey;
import com.alvis.media.domain.VideoInfo;
import com.alvis.media.repository.UserVideoOperationMapper;
import com.alvis.media.repository.VideoInfoMapper;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController("AdminCollectionController")
@RequestMapping("/api/collection")
@AllArgsConstructor
public class CollectionController extends BaseApiController {

    private final UserVideoOperationMapper operationMapper;
    private final VideoInfoMapper videoInfoMapper;

    @PostMapping("/toggle")
    public RestResponse<Integer> toggle(@RequestParam Integer videoId) {
        Integer userId = getCurrentUser().getId();
        UserVideoOperationKey key = new UserVideoOperationKey();
        key.setId(userId);
        key.setVideoId(videoId);
        UserVideoOperation old = operationMapper.selectByPrimaryKey(key);
        int now;
        if (old == null) {
            UserVideoOperation op = new UserVideoOperation();
            op.setId(userId);
            op.setVideoId(videoId);
            op.setCollection(1);
            op.setThumbUp(0);
            operationMapper.insertSelective(op);
            now = 1;
        } else {
            now = (old.getCollection() != null && old.getCollection() == 1) ? 0 : 1;
            old.setCollection(now);
            operationMapper.updateByPrimaryKeySelective(old);
        }
        return RestResponse.ok(now);
    }

    @PostMapping("/list")
    public RestResponse<List<VideoInfo>> list() {
        Integer userId = getCurrentUser().getId();
        List<Integer> ids = operationMapper.selectCollections(userId).stream()
                .map(UserVideoOperation::getVideoId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return RestResponse.ok(java.util.Collections.emptyList());
        }
        return RestResponse.ok(videoInfoMapper.selectByIds(ids));
    }
}
