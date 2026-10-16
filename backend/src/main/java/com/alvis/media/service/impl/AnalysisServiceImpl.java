package com.alvis.media.service.impl;

import com.alvis.media.repository.UserVideoOperationMapper;
import com.alvis.media.repository.VideoInfoMapper;
import com.alvis.media.service.AnalysisService;
import com.alvis.media.viewmodel.analysis.AnalysisVM;
import com.alvis.media.viewmodel.analysis.UserAnalysisVM;
import com.alvis.media.viewmodel.analysis.VideoAnalysisVM;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author 奇趣
 */
@Service
public class AnalysisServiceImpl implements AnalysisService {

    private final VideoInfoMapper videoInfoMapper;
    private final UserVideoOperationMapper userVideoOperationMapper;

    @Autowired
    public AnalysisServiceImpl(VideoInfoMapper videoInfoMapper,
                               UserVideoOperationMapper userVideoOperationMapper) {
        this.videoInfoMapper = videoInfoMapper;
        this.userVideoOperationMapper = userVideoOperationMapper;
    }

    @Override
    public AnalysisVM overview() {
        AnalysisVM vm = new AnalysisVM();
        vm.setGenreDistribution(videoInfoMapper.selectGenreDistribution());
        vm.setYearDistribution(videoInfoMapper.selectYearDistribution());
        vm.setBudgetRevenue(videoInfoMapper.selectBudgetRevenue());
        vm.setRatingDistribution(userVideoOperationMapper.selectRatingDistribution());
        vm.setActiveUsers(userVideoOperationMapper.selectActiveUsers());
        return vm;
    }

    @Override
    public VideoAnalysisVM videoOverview() {
        VideoAnalysisVM vm = new VideoAnalysisVM();
        vm.setStats(videoInfoMapper.selectVideoStats());
        vm.setGenreDistribution(videoInfoMapper.selectGenreDistribution());
        vm.setYearDistribution(videoInfoMapper.selectYearDistribution());
        vm.setBudgetRevenue(videoInfoMapper.selectBudgetRevenuePoints());
        vm.setRuntimeDistribution(videoInfoMapper.selectRuntimeDistribution());
        vm.setLanguageDistribution(videoInfoMapper.selectLanguageDistribution());
        vm.setTopRevenue(videoInfoMapper.selectTopRevenue(10));
        vm.setCountryDistribution(videoInfoMapper.selectCountryDistribution());
        vm.setStudioTop(videoInfoMapper.selectStudioTop(12));
        vm.setSourceDistribution(videoInfoMapper.selectSourceDistribution());
        vm.setTypeDistribution(videoInfoMapper.selectTypeDistribution());
        return vm;
    }

    @Override
    public UserAnalysisVM userOverview() {
        UserAnalysisVM vm = new UserAnalysisVM();
        vm.setStats(userVideoOperationMapper.selectUserStats());
        vm.setRatingDistribution(userVideoOperationMapper.selectRatingDistribution());
        vm.setActiveUsers(userVideoOperationMapper.selectActiveUsers());
        vm.setRatingCountDistribution(userVideoOperationMapper.selectRatingCountDistribution());
        vm.setUserAvgRatingDistribution(userVideoOperationMapper.selectUserAvgRatingDistribution());
        return vm;
    }
}