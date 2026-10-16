package com.alvis.media.viewmodel.dataset;

import lombok.Data;

import java.util.Map;

/**
 * 训练任务返回对象：含评估指标与可读状态名
 */
@Data
public class TrainTaskVM {
    private Long taskId;
    private String algo;
    private String algoType;
    private Integer datasetId;
    private String status;
    private String statusName;
    private Integer progress;
    private Integer rowCount;
    private Double trainSeconds;
    private String cacheFile;
    private String message;
    private String traceId;
    private Integer submitterId;
    private String startTime;
    private String finishTime;
    private String createTime;
    /** 评估指标：rmse / mae / r2 / r2_log */
    private Map<String, Double> metrics;
}
