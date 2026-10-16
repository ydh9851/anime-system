package com.alvis.media.service;

import com.alvis.media.domain.other.KeyValue;
import com.alvis.media.viewmodel.dataset.TrainRequestVM;
import com.alvis.media.viewmodel.dataset.TrainTaskVM;

import java.util.List;

/**
 * 模型训练管理Service：提交训练任务 / 查询任务状态 / 查看评估指标
 */
public interface ModelTrainService {

    /**
     * 提交训练任务（异步执行，立即返回 taskId）
     *
     * @param vm          训练参数
     * @param submitterId 提交人 id
     * @param traceId发起请求的 traceId，用于串联日志
     * @return 已入库的任务（含 taskId 与初始状态）
     */
    TrainTaskVM submit(TrainRequestVM vm, Integer submitterId, String traceId);

    /**
     * 全部训练任务
     */
    List<TrainTaskVM> listTasks();

    /**
     * 任务详情（含评估指标）
     *
     * @param taskId 任务 id
     * @return TrainTaskVM，不存在返回 null
     */
    TrainTaskVM taskDetail(Long taskId);

    /**
     * 各状态任务数
     */
    List<KeyValue> countGroupByStatus();

    /**
     * 支持的算法列表
     */
    List<KeyValue> listAlgos();
}
