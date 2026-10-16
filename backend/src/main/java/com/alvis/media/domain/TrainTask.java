package com.alvis.media.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 模型训练任务：异步执行 + 状态机 + 评估指标落库
 */
@Data
@EqualsAndHashCode
@TableName("t_train_task")
public class TrainTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "task_id", type = IdType.AUTO)
    private Long taskId;

    private String algo;

    private String algoType;

    private Integer datasetId;

    private String status;

    private Integer progress;

    private Integer rowCount;

    private Double trainSeconds;

    private String metricsJson;

    private String cacheFile;

    private String message;

    private String traceId;

    private Integer submitterId;

    private Date startTime;

    private Date finishTime;

    private Date createTime;
}
