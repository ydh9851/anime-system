package com.alvis.media.viewmodel.dataset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 提交训练任务入参
 */
@Data
public class TrainRequestVM {
    @NotBlank(message = "请选择算法")
    @Pattern(regexp = "^(lr|knn|svm|dt|rf|lgbm)$", message = "算法仅支持 lr / knn / svm / dt / rf / lgbm")
    private String algo;

    /** 数据集 id，为空表示使用工程默认数据集 data/train.csv */
    private Integer datasetId;

    /** true 表示只用缓存模型评估，不重新训练 */
    private Boolean evalOnly;
}
