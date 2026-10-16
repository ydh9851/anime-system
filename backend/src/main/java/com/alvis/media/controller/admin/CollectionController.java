package com.alvis.media.controller.admin;

import com.alvis.media.base.BaseApiController;
import com.alvis.media.base.RestResponse;
import com.alvis.media.base.SystemCode;
import com.alvis.media.domain.VideoInfo;
import com.alvis.media.service.UserVideoOperationService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收藏接口。原先直接注入 UserVideoOperationMapper / VideoInfoMapper，
 * 现改为调用 Service 层，Controller 只做参数接收与响应封装。
 */
@RestController("AdminCollectionController")
@RequestMapping("/api/collection")
@AllArgsConstructor
public class CollectionController extends BaseApiController {

    private final UserVideoOperationService userVideoOperationService;

    /**
     * 切换收藏状态
     *
     * @param videoId 动漫 id
     * @return 切换后状态：1 已收藏，0 未收藏
     */
    @PostMapping("/toggle")
    public RestResponse<Integer> toggle(@RequestParam Integer videoId) {
        if (getCurrentUser() == null) {
            return RestResponse.fail(SystemCode.UNAUTHORIZED.getCode(), "用户未登录");
        }
        return RestResponse.ok(userVideoOperationService.toggleCollection(getCurrentUser().getId(), videoId));
    }

    /**
     * 当前登录用户的收藏列表
     */
    @PostMapping("/list")
    public RestResponse<List<VideoInfo>> list() {
        if (getCurrentUser() == null) {
            return RestResponse.fail(SystemCode.UNAUTHORIZED.getCode(), "用户未登录");
        }
        return RestResponse.ok(userVideoOperationService.listCollectionVideos(getCurrentUser().getId()));
    }

    /**
     * 查询当前用户是否已收藏某动漫（详情页按钮回显）
     */
    @GetMapping("/{videoId}")
    public RestResponse<Boolean> isCollected(@PathVariable Integer videoId) {
        if (getCurrentUser() == null) {
            return RestResponse.fail(SystemCode.UNAUTHORIZED.getCode(), "用户未登录");
        }
        Integer userId = getCurrentUser().getId();
        return RestResponse.ok(userVideoOperationService.listCollectionVideos(userId).stream()
                .anyMatch(v -> v.getVideoId() != null && v.getVideoId().equals(videoId)));
    }
}
