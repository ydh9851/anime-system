package com.alvis.media.service;

import com.alvis.media.viewmodel.analysis.AnalysisVM;
import com.alvis.media.viewmodel.analysis.UserAnalysisVM;
import com.alvis.media.viewmodel.analysis.VideoAnalysisVM;

/**
 * 数据分析看板Service。
 * 原先 AnalysisController 直接注入 VideoInfoMapper / UserVideoOperationMapper，
 * 抽出本接口后Controller 只负责参数接收与响应封装，符合三层架构职责划分。
 */
public interface AnalysisService {

    /**
     * 看板总览（旧版概览）
     *
     * @return AnalysisVM
     */
    AnalysisVM overview();

    /**
     * 动漫属性维度分析：类型/年份/集数热度/时长/语言/热度TOP/制作公司等
     *
     * @return VideoAnalysisVM
     */
    VideoAnalysisVM videoOverview();

    /**
     * 用户追番行为维度分析：评分分布/活跃用户/评分次数/用户均分
     *
     * @return UserAnalysisVM
     */
    UserAnalysisVM userOverview();
}