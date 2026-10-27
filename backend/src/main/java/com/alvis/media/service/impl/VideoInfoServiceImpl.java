package com.alvis.media.service.impl;

import com.alvis.media.domain.Tag;
import com.alvis.media.domain.VideoInfo;
import com.alvis.media.domain.VideoPlay;
import com.alvis.media.repository.MediaBaseMapper;
import com.alvis.media.repository.VideoInfoMapper;
import com.alvis.media.repository.VideoPlayMapper;
import com.alvis.media.service.UserVideoOperationService;
import com.alvis.media.service.VideoInfoService;
import com.alvis.media.service.VideoTagService;
import com.alvis.media.utility.DateTimeUtil;
import com.alvis.media.utility.UserCFUtil;
import com.alvis.media.viewmodel.video.VideoFilterOptionsResponseVM;
import com.alvis.media.viewmodel.video.VideoPageRequestVM;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 奇趣
 */
@Service

public class VideoInfoServiceImpl extends BaseServiceImpl <VideoInfo> implements VideoInfoService {

    private final VideoInfoMapper videoInfoMapper;
    private final VideoPlayMapper videoPlayMapper;
    private final VideoTagService videoTagService;
    private final UserVideoOperationService userVideoOperationService;

    @Autowired
    public VideoInfoServiceImpl(VideoInfoMapper videoInfoMapper,
                                VideoPlayMapper videoPlayMapper,
                                MediaBaseMapper<VideoInfo> videoInfoMapperr,
                                VideoTagService videoTagService,
                                UserVideoOperationService userVideoOperationService) {
        super(videoInfoMapperr);
        this.videoInfoMapper = videoInfoMapper;
        this.videoPlayMapper = videoPlayMapper;
        this.videoTagService = videoTagService;
        this.userVideoOperationService = userVideoOperationService;

    }

    @Override
    public int insertByFilter(VideoInfo record) {
        record.setLastModifyTime(new Date());
        record.setCreateTime(new Date());
        return baseMapper.insertSelective(record);
    }

    @Override
    public int selectNewVideoCount(VideoInfo filter) {
        //当月的用户新增视频数量
        //封装查询条件
        filter.setCreateTime(DateTimeUtil.getMonthStartDay());
        // 调用dao 并返回
        return videoInfoMapper.selectNewVideoCount(filter);
    }

    @Override
    public int countAll() {
        return videoInfoMapper.countAll();
    }


    /** 地区 / 语言 / 类型 / 题材 的取值映射，用于把库里的编码翻译成中文标签 */
    private static final Map<String, String> COUNTRY_LABEL = new LinkedHashMap<>();
    private static final Map<String, String> LANGUAGE_LABEL = new LinkedHashMap<>();
    private static final Map<String, String> TYPE_LABEL = new LinkedHashMap<>();
    private static final Map<String, String> GENRE_LABEL = new LinkedHashMap<>();

    static {
        COUNTRY_LABEL.put("JP", "日本");
        COUNTRY_LABEL.put("CN", "中国");
        COUNTRY_LABEL.put("KR", "韩国");
        COUNTRY_LABEL.put("US", "欧美");
        COUNTRY_LABEL.put("OTHER", "其他");
        LANGUAGE_LABEL.put("ja", "日语");
        LANGUAGE_LABEL.put("zh", "国语");
        LANGUAGE_LABEL.put("ko", "韩语");
        LANGUAGE_LABEL.put("en", "英语");
        LANGUAGE_LABEL.put("OTHER", "其他");
        TYPE_LABEL.put("TV", "TV 动画");
        TYPE_LABEL.put("Movie", "剧场版");
        TYPE_LABEL.put("OVA", "OVA");
        TYPE_LABEL.put("ONA", "网络动画");
        TYPE_LABEL.put("Special", "特别篇");
        TYPE_LABEL.put("Music", "音乐");
        GENRE_LABEL.put("Action", "动作");
        GENRE_LABEL.put("Adventure", "冒险");
        GENRE_LABEL.put("Comedy", "喜剧");
        GENRE_LABEL.put("Drama", "剧情");
        GENRE_LABEL.put("Fantasy", "奇幻");
        GENRE_LABEL.put("Horror", "恐怖");
        GENRE_LABEL.put("Mystery", "推理");
        GENRE_LABEL.put("Romance", "恋爱");
        GENRE_LABEL.put("Sci-Fi", "科幻");
        GENRE_LABEL.put("Slice of Life", "日常");
        GENRE_LABEL.put("Sports", "运动");
        GENRE_LABEL.put("Supernatural", "超自然");
        GENRE_LABEL.put("Suspense", "悬疑");
        GENRE_LABEL.put("Award Winning", "获奖");
        GENRE_LABEL.put("Avant Garde", "先锋");
        GENRE_LABEL.put("Boys Love", "BL");
        GENRE_LABEL.put("Girls Love", "GL");
        GENRE_LABEL.put("Gourmet", "美食");
    }

    @Override
    public VideoFilterOptionsResponseVM filterOptions() {
        VideoFilterOptionsResponseVM vm = new VideoFilterOptionsResponseVM();
        vm.setTotal(videoInfoMapper.countAll());
        vm.setCountries(toOptions(videoInfoMapper.selectFilterCountries(), COUNTRY_LABEL));
        vm.setLanguages(toOptions(videoInfoMapper.selectFilterLanguages(), LANGUAGE_LABEL));
        vm.setTypes(toOptions(videoInfoMapper.selectFilterTypes(), TYPE_LABEL));
        vm.setGenres(toOptions(videoInfoMapper.selectFilterGenres(), GENRE_LABEL));
        Map<String, Object> bounds = videoInfoMapper.selectFilterYearBounds();
        if (bounds != null) {
            Object min = bounds.get("minYear");
            Object max = bounds.get("maxYear");
            vm.setMinYear(min == null ? null : Integer.valueOf(String.valueOf(min)));
            vm.setMaxYear(max == null ? null : Integer.valueOf(String.valueOf(max)));
        }
        return vm;
    }

    private List<VideoFilterOptionsResponseVM.Option> toOptions(List<Map<String, Object>> rows,
                                                               Map<String, String> labelMap) {
        List<VideoFilterOptionsResponseVM.Option> list = new ArrayList<>();
        if (rows == null) {
            return list;
        }
        for (Map<String, Object> row : rows) {
            Object nameObj = row.get("name");
            Object countObj = row.get("count");
            if (nameObj == null) {
                continue;
            }
            VideoFilterOptionsResponseVM.Option opt = new VideoFilterOptionsResponseVM.Option();
            opt.setValue(String.valueOf(nameObj));
            opt.setLabel(labelMap.getOrDefault(opt.getValue(), opt.getValue()));
            opt.setCount(countObj == null ? 0 : Integer.valueOf(String.valueOf(countObj)));
            list.add(opt);
        }
        return list;
    }
    @Override
    public PageInfo <VideoInfo> userPage(VideoPageRequestVM requestVM) {
        // 注意：不能给 PageHelper 传第三个 orderBy 参数，它会覆盖 SQL 里的 ORDER BY，
        // 导致前端选择的排序方式（评分 / 热度 / 首播时间）全部失效。
        // 排序统一由 VideoInfoMapper.xml 的 <choose> 决定，默认分支为 video_id desc。
        return PageHelper.startPage(requestVM.getPageIndex(), requestVM.getPageSize()).doSelectPageInfo(() ->
                videoInfoMapper.videoPage(requestVM)
        );
    }

    @Override
    public List <VideoInfo> relatedVideos(Integer videoId, Integer limit) {
        List<VideoInfo> list = videoInfoMapper.selectRelatedVideos(videoId,
                limit == null || limit <= 0 ? 6 : Math.min(limit, 24));
        return list == null ? new ArrayList<>() : list;
    }

    @Override
    public List <VideoInfo> userAnalysis(Integer recommendUserId) {

        List <VideoInfo> result = new ArrayList <>();
        //和该推荐用户的播放记录有交集的用户的播放记录。
        //第一步查找recommendUserId的播放列表 userid
        //要求推荐的用户播放的视频id列表
        List <Integer> recommendVideoIdPlayList = getPlayVideoIdListByUserId(recommendUserId);
        if (CollectionUtils.isEmpty(recommendVideoIdPlayList)) {
            return videoInfoMapper.selectHotVideoList();
        }

        Set <Integer> relationUserIdList = new HashSet <>();
        //遍历recommendVideoIdPlayList 去查找播放过这些视频的用户
        for (Integer videoId : recommendVideoIdPlayList) {
            VideoPlay filter = new VideoPlay();

            filter.setVideoId(videoId);
            List <VideoPlay> otherUserPlayList = videoPlayMapper.selectVideoPlayInfo(filter);

            if (CollectionUtils.isEmpty(otherUserPlayList)) {
                continue;
            }
            relationUserIdList.addAll(otherUserPlayList.stream()
                    .map(e -> e.getUserId()).collect(Collectors.toSet()));
        }

        //和该推荐用户的播放记录有交集的用户的播放记录。
        Map <Integer, List <Integer>> playList = new HashMap <>();
        playList.put(recommendUserId, recommendVideoIdPlayList);

        for (Integer userId : relationUserIdList) {
            if (userId.equals(recommendUserId)) {
                continue;
            }
            //要求推荐的用户播放的视频id列表
            playList.put(userId, getPlayVideoIdListByUserId(userId));
        }

        List <Integer> recommendVideoIdList = UserCFUtil.getRecommendationVideoList(playList, recommendUserId);
        //如果没有可推荐的
        if(CollectionUtils.isEmpty(recommendVideoIdList))
        {
          return  videoInfoMapper.selectHotVideoList();
        }
        for (Integer videoId : recommendVideoIdList) {
            VideoInfo videoInfo = videoInfoMapper.selectByPrimaryKey(videoId);
            // 过滤数据库中不存在的视频，避免前端拿到 null
            if (videoInfo != null) {
                result.add(videoInfo);
            }
        }
        return result;
    }

    private List <Integer> getPlayVideoIdListByUserId(Integer userId) {
        VideoPlay filter = new VideoPlay();
        filter.setUserId(userId);
        List <VideoPlay> recommendUserPlayList = videoPlayMapper.selectVideoPlayInfo(filter);
        //如果没有播放列表，则没有推荐产品
        if (CollectionUtils.isEmpty(recommendUserPlayList)) {
            return new ArrayList <>();
        }
        //要求推荐的用户播放的视频id列表
        List <Integer> recommendVideoIdPlayList = recommendUserPlayList.stream()
                .map(e -> e.getVideoId()).collect(Collectors.toList());

        return recommendVideoIdPlayList;
    }

    @Override
    public int insertVideoPlay(String realUrl, Integer userId) {
        // 视频地址为空时不记录播放（避免 selectVideoIdByUrl('') 误匹配全表导致异常）
        if (StringUtils.isBlank(realUrl)) {
            return 0;
        }
        //TODO
        //step1 组装数据
        VideoPlay videoPlay = new VideoPlay();
        videoPlay.setUserId(userId);
        //根据user_url获取videoId，
        videoPlay.setVideoId(videoInfoMapper.selectVideoIdByUrl(realUrl));
        //根据videoId和userId获取播放次数，
        List <VideoPlay> videoPlayListDb = videoPlayMapper.selectVideoPlayInfo(videoPlay);
        videoPlay.setLastPlayTime(new Date());
        if (CollectionUtils.isEmpty(videoPlayListDb)) {
            //insert
            videoPlay.setPlayTimes(1);
            videoPlayMapper.insertSelective(videoPlay);
        } else {
            VideoPlay videoPlayDb = videoPlayListDb.get(0);
            videoPlay.setPlayTimes(videoPlayDb.getPlayTimes() + 1);
            videoPlayMapper.updateByPrimaryKeySelective(videoPlay);
        }

        // 往t_video_paly这表insert一条数据或者update.
        return 0;
    }

    @Override
    public int recalculateHeat() {
        // IMDB 加权分: score = v/(v+m)*r + m/(m+v)*c, 10 分制
        List<VideoInfo> all = videoInfoMapper.selectAll();
        double m = all.stream().mapToDouble(v -> v.getVoteCount() == null ? 0 : v.getVoteCount()).average().orElse(1);
        double c = all.stream().mapToDouble(v -> v.getVoteAverage() == null ? 0 : v.getVoteAverage()).average().orElse(0);
        double maxPop = all.stream().mapToDouble(v -> v.getPopularity() == null ? 0 : v.getPopularity()).max().orElse(1);
        double minPop = all.stream().mapToDouble(v -> v.getPopularity() == null ? 0 : v.getPopularity()).min().orElse(0);
        int updated = 0;
        for (VideoInfo v : all) {
            double vc = v.getVoteCount() == null ? 0 : v.getVoteCount();
            double r  = v.getVoteAverage() == null ? 0 : v.getVoteAverage();
            double imdb = (vc / (vc + m)) * r + (m / (m + vc)) * c;
            double pop = v.getPopularity() == null ? 0 : v.getPopularity();
            double normPop = (maxPop == minPop) ? 0 : (pop - minPop) / (maxPop - minPop) * 10;
            double play = vc; // 简化: 投票数≈热度（计划备注）
            double heat = 0.5 * Math.log10(1 + play) + 0.3 * imdb + 0.2 * normPop;
            if (Double.isNaN(heat) || Double.isInfinite(heat)) {
                heat = 0.0;
            }
            v.setHeatScore(heat);
            videoInfoMapper.updateByPrimaryKeySelective(v);
            updated++;
        }
        return updated;
    }

    @Override
    public List<VideoInfo> heatTop(Integer limit) {
        if (limit == null || limit <= 0) limit = 10;
        return videoInfoMapper.selectHeatTop(limit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateVideo(VideoInfo record, List<Tag> tagList) {
        if (record == null || record.getVideoId() == null) {
            return 0;
        }
        record.setLastModifyTime(new Date());
        int affected = videoInfoMapper.updateByPrimaryKeySelective(record);
        // tagList 为 null 表示本次不改动标签；传空集合表示清空标签
        if (tagList != null) {
            videoTagService.replaceTags(record.getVideoId(), tagList);
        }
        return affected;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteVideo(Integer videoId) {
        if (videoId == null) {
            return false;
        }
        if (videoInfoMapper.selectByPrimaryKey(videoId) == null) {
            return false;
        }
        // 外键均为 RESTRICT：必须「先子表后主表」，否则抛 SQLIntegrityConstraintViolationException
        videoTagService.deleteByVideoId(videoId);
        videoPlayMapper.deleteByVideoId(videoId);
        userVideoOperationService.deleteByVideoId(videoId);
        return videoInfoMapper.deleteByPrimaryKey(videoId) > 0;
    }

    @Override
    public List<VideoInfo> selectByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return videoInfoMapper.selectByIds(ids);
    }

    @Override
    public List<VideoInfo> selectByNameLike(String name, Integer limit) {
        int max = (limit == null || limit <= 0) ? 20 : Math.min(limit, 100);
        return videoInfoMapper.selectByNameLike(StringUtils.defaultString(name).trim(), max);
    }

//
//        VideoPlay videoPlay = new VideoPlay();
//        videoPlay.setUserId(userId);
//
//        videoPlay.setVideoId(videoInfoMapper.selectVideoIdByUrl(realUrl));
//
//        List <VideoPlay> videoPlayDb = videoPlayMapper.selectByUserIdAndVideoId(videoPlay);
//            videoPlay.setLastPlayTime(new Date());
//            videoPlay.setPlayTimes(1);
//            videoPlayMapper.insertSelective(videoPlay);
//            videoPlay.setLastPlayTime(new Date());
//            videoPlay.setPlayTimes(videoPlayDb.get(0).getPlayTimes() + 1);
//            videoPlayMapper.updateByPrimaryKeySelective(videoPlay);
}
