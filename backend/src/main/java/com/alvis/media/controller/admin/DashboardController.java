package com.alvis.media.controller.admin;

import com.alvis.media.base.BaseApiController;
import com.alvis.media.base.RestResponse;
import com.alvis.media.domain.User;
import com.alvis.media.domain.VideoInfo;
import com.alvis.media.domain.VideoPlay;
import com.alvis.media.repository.VideoInfoMapper;
import com.alvis.media.service.UserEventLogService;
import com.alvis.media.service.UserService;
import com.alvis.media.service.VideoInfoService;
import com.alvis.media.service.VideoPlayService;
import com.alvis.media.utility.DateTimeUtil;
import com.alvis.media.viewmodel.admin.dashboard.DistributionVM;
import com.alvis.media.viewmodel.admin.dashboard.IndexVM;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("AdminDashboardController")
@RequestMapping(value = "/api/admin/dashboard")
@AllArgsConstructor
public class DashboardController extends BaseApiController {

    private final UserService userService;
    private final VideoInfoService videoInfoService;
    private final VideoPlayService videoPlayService;
    private final UserEventLogService userEventLogService;
    private final VideoInfoMapper videoInfoMapper;


    @RequestMapping(value = "/index", method = RequestMethod.POST)
    public RestResponse<IndexVM> Index() {
        IndexVM vm = new IndexVM();

       // 查询本月新增用户，
        // step1:字段封装
       //  step2：调用service层得到结果
        int count = userService.selectUserCount(new User());
       // step3: 将结果封装返回给前端。
        vm.setNewUserCount(count);



       // 动漫库总数。本工程的 t_video_info 是一次性全量导入的（24661 部），
       // 统计「本月新增」恒为 0、没有展示价值，因此这里展示库内总量。
       int videoTotal = videoInfoService.countAll();
       vm.setVideoTotal(videoTotal);

       // 查询本月视频播放数
       // 1.调用service 去数据库查找数据
       int videoPlayCount = videoPlayService.selectVideoPlayCount(new VideoPlay());

       // 2.封装返回信息
       vm.setDoPlayVideoCount(videoPlayCount);

        // 查询本月最佳影片
        // 1.调用service 去数据库查找数据
        String bestVideo = videoPlayService.selectBestVideo(new VideoPlay());

        // 2.封装返回信息
        vm.setHotVideoCount(bestVideo);

        List<Integer> mothDayUserActionValue = userEventLogService.selectMothCount();
		
        List<Integer> mothDayVideoPlayValue = videoPlayService.selectMothCount();
		
        vm.setMothDayUserActionValue(mothDayUserActionValue);
        vm.setMothDayVideoPlayValue(mothDayVideoPlayValue);

        vm.setHeatTopList(videoInfoService.heatTop(10));

        vm.setMothDayText(DateTimeUtil.MothDay());
        return RestResponse.ok(vm);
    }

    @RequestMapping(value = "/heat-top", method = RequestMethod.POST)
    public RestResponse<List<VideoInfo>> heatTop(@RequestParam(defaultValue = "10") Integer limit) {
        return RestResponse.ok(videoInfoService.heatTop(limit));
    }

    /**
     * 首页看板可视化分布数据（RESTful 查询用GET，不涉及写操作故未用 POST）。
     *
     * 复用 VideoInfoMapper 里已有的统计 SQL，不新增查询逻辑：
     * yearDistribution -> 柱状图；typeDistribution / genreDistribution -> 饼图；topRevenue -> 横向柱状图。
     */
    @GetMapping("/distribution")
    public RestResponse<DistributionVM> distribution() {
        DistributionVM vm = new DistributionVM();
        vm.setYearDistribution(videoInfoMapper.selectYearDistribution());
        vm.setTypeDistribution(videoInfoMapper.selectTypeDistribution());
        vm.setGenreDistribution(videoInfoMapper.selectGenreDistribution());
        vm.setLanguageDistribution(videoInfoMapper.selectLanguageDistribution());
        vm.setTopRevenue(videoInfoMapper.selectTopRevenue(10));
        return RestResponse.ok(vm);
    }

    @RequestMapping(value = "/recalculate-heat", method = RequestMethod.POST)
    public RestResponse<Integer> recalculateHeat() {
        return RestResponse.ok(videoInfoService.recalculateHeat());
    }
}
