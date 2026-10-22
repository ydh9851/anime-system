package com.alvis.media.controller;

import com.alvis.media.base.BaseApiController;
import com.alvis.media.base.NonStaticResourceHttpRequestHandler;
import com.alvis.media.base.RestResponse;
import com.alvis.media.base.SystemCode;
import com.alvis.media.domain.*;
import com.alvis.media.domain.other.KeyValue;
import com.alvis.media.service.VideoInfoService;
import com.alvis.media.service.VideoTagService;
import com.alvis.media.utility.PageInfoHelper;
import com.alvis.media.viewmodel.video.*;
import com.github.pagehelper.PageInfo;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 动漫（数据集条目）管理接口。
 *
 * RESTful 设计：
 * POST   /api/admin/video                    新增
 * PUT    /api/admin/video/{id}               修改
 * DELETE /api/admin/video/{id}               删除
 * GET    /api/admin/video/{id}               详情（含播放记录与标签）
 * GET    /api/admin/video/{id}/basic         基础详情（编辑页回填，不产生播放记录）
 * GET    /api/admin/video/{id}/user-analysis 相似用户偏好分析
 * POST   /api/admin/video/page/list          分页查询（入参含分页+筛选条件，故用 POST body）
 * GET    /api/admin/video/name               按名称模糊查询（下拉选择）
 * GET    /api/admin/video/play/**            视频流回放
 */
@RestController("VideoController")
@RequestMapping(value = "/api/admin/video")
@AllArgsConstructor
public class VideoController extends BaseApiController {

    private final VideoInfoService videoInfoService;

    private final VideoTagService videoTagService;

    @Autowired
    private NonStaticResourceHttpRequestHandler nonStaticResourceHttpRequestHandler;

    @GetMapping("/play/**")
    public void playVideo(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String url = request.getRequestURL().toString();
        int index = url.lastIndexOf("D:");
        String realPath = url.substring(index);

        Path filePath = Paths.get(realPath);
        if (Files.exists(filePath)) {
            String mimeType = Files.probeContentType(filePath);
            if (!StringUtils.isEmpty(mimeType)) {
                response.setContentType(mimeType);
            }
            request.setAttribute(nonStaticResourceHttpRequestHandler.filepath, filePath);
            nonStaticResourceHttpRequestHandler.handleRequest(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
        }
    }

    @PostMapping(value = "/page/list")
    public RestResponse<PageInfo<VideoResponseVM>> pageList(@RequestBody VideoPageRequestVM model) {
        PageInfo<VideoInfo> pageInfo = videoInfoService.userPage(model);
        PageInfo<VideoResponseVM> page = PageInfoHelper.copyMap(pageInfo, VideoResponseVM::from);
        return RestResponse.ok(page);
    }

    /**
     * 动漫库筛选面板元数据：地区 / 语言 / 类型 / 题材选项及其命中数量。
     * 前端据此渲染多维筛选面板，选项顺序按数量降序。
     */
    @GetMapping(value = "/filter-options")
    public RestResponse<VideoFilterOptionsResponseVM> filterOptions() {
        return RestResponse.ok(videoInfoService.filterOptions());
    }

    @GetMapping(value = "/name")
    public RestResponse<List<KeyValue>> selectByVideoName(@RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) Integer limit) {
        if (StringUtils.isBlank(keyword)) {
            return RestResponse.ok(List.of());
        }
        List<KeyValue> list = videoInfoService.selectByNameLike(keyword, limit).stream()
                .map(v -> {
                    KeyValue kv = new KeyValue();
                    kv.setName(v.getVideoName());
                    kv.setValue(v.getVideoId());
                    return kv;
                }).collect(Collectors.toList());
        return RestResponse.ok(list);
    }

    @PostMapping
    public RestResponse<VideoResponseVM> createVideo(@RequestBody @Valid VideoCreateVM model) {
        if (StringUtils.isBlank(model.getVideoUrl())) {
            return RestResponse.fail(SystemCode.ParameterValidError.getCode(), "视频封面未上传");
        }
        VideoInfo video = modelMapper.map(model, VideoInfo.class);
        video.setCreatorId(getCurrentUser().getId());
        videoInfoService.insertByFilter(video);
        if (model.getVideoTagList() != null && !model.getVideoTagList().isEmpty()) {
            videoTagService.replaceTags(video.getVideoId(), model.getVideoTagList());
        }
        return RestResponse.ok(VideoResponseVM.from(video));
    }

    @PutMapping(value = "/{id}")
    public RestResponse<VideoResponseVM> updateVideo(@PathVariable Integer id,
                                                    @RequestBody @Valid VideoUpdateVM model) {
        if (videoInfoService.selectById(id) == null) {
            return RestResponse.fail(SystemCode.InnerError.getCode(), "未找到该动漫");
        }
        VideoInfo video = new VideoInfo();
        video.setVideoId(id);
        video.setVideoName(model.getVideoName());
        video.setVideoCategory(model.getVideoCategory());
        if (StringUtils.isNotBlank(model.getVideoUrl())) {
            video.setVideoUrl(model.getVideoUrl());
        }
        videoInfoService.updateVideo(video, model.getVideoTagList());

        VideoResponseVM vm = VideoResponseVM.from(videoInfoService.selectById(id));
        vm.setVideoTagList(videoTagService.selectTagsByVideoId(id));
        return RestResponse.ok(vm);
    }

    @DeleteMapping(value = "/{id}")
    public RestResponse<Void> deleteVideo(@PathVariable Integer id) {
        if (!videoInfoService.deleteVideo(id)) {
            return RestResponse.fail(SystemCode.InnerError.getCode(), "未找到该动漫或已被删除");
        }
        return RestResponse.ok();
    }

    @GetMapping(value = "/{id}")
    public RestResponse<VideoResponseVM> select(@PathVariable Integer id) {
        VideoInfo video = videoInfoService.selectById(id);
        if (video == null) {
            return RestResponse.fail(SystemCode.InnerError.getCode(), "未找到该动漫");
        }
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            videoInfoService.insertVideoPlay(video.getVideoUrl(), currentUser.getId());
        }
        if (StringUtils.isNotBlank(video.getVideoUrl())) {
            video.setVideoUrl("/api/admin/video/play/" + video.getVideoUrl());
        }
        VideoResponseVM vm = VideoResponseVM.from(video);
        vm.setVideoTagList(videoTagService.selectTagsByVideoId(id));
        return RestResponse.ok(vm);
    }

    @GetMapping(value = "/{id}/basic")
    public RestResponse<VideoResponseVM> getVideoDetailByVideoId(@PathVariable Integer id) {
        VideoInfo video = videoInfoService.selectById(id);
        if (video == null) {
            return RestResponse.fail(SystemCode.InnerError.getCode(), "未找到该动漫");
        }
        VideoResponseVM vm = VideoResponseVM.from(video);
        vm.setVideoTagList(videoTagService.selectTagsByVideoId(id));
        return RestResponse.ok(vm);
    }

    @GetMapping(value = "/{id}/user-analysis")
    public RestResponse<UserAnalysisVM> userAnalysis(@PathVariable Integer id) {
        UserAnalysisVM vm = new UserAnalysisVM();
        vm.setVideoRecommendList(videoInfoService.userAnalysis(id));
        return RestResponse.ok(vm);
    }
}
