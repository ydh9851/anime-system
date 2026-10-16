package com.alvis.media.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 训练数据集（上传的 CSV 元信息）
 */
@Data
@EqualsAndHashCode
@TableName("t_dataset")
public class Dataset implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "dataset_id", type = IdType.AUTO)
    private Integer datasetId;

    private String name;

    private String fileName;

    private String filePath;

    private Long fileSize;

    private Integer rowCount;

    private String columnsDesc;

    private String remark;

    private Integer uploaderId;

    private Date createTime;
}
