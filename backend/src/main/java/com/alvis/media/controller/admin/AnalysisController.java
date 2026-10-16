package com.alvis.media.controller.admin;

import com.alvis.media.base.BaseApiController;
import com.alvis.media.base.RestResponse;
import com.alvis.media.service.AnalysisService;
import com.alvis.media.viewmodel.analysis.AnalysisVM;
import com.alvis.media.viewmodel.analysis.UserAnalysisVM;
import com.alvis.media.viewmodel.analysis.VideoAnalysisVM;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据分析看板接口。原先直接注入 VideoInfoMapper / UserVideoOperationMapper，
 * 现改为调用 AnalysisService，Controller 只负责请求转发与响应封装。
 */
@RestController("AdminAnalysisController")
@RequestMapping(value = "/api/admin/analysis")
@AllArgsConstructor
public class AnalysisController extends BaseApiController {

    private final AnalysisService analysisService;

    @GetMapping("/overview")
    public RestResponse<AnalysisVM> overview() {
        return RestResponse.ok(analysisService.overview());
    }

    @GetMapping("/video-overview")
    public RestResponse<VideoAnalysisVM> videoOverview() {
        return RestResponse.ok(analysisService.videoOverview());
    }

    @GetMapping("/user-overview")
    public RestResponse<UserAnalysisVM> userOverview() {
        return RestResponse.ok(analysisService.userOverview());
    }
}
