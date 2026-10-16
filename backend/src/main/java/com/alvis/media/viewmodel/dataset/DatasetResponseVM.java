package com.alvis.media.viewmodel.dataset;

import lombok.Data;

/**
 * 数据集列表 / 详情返回对象
 */
@Data
public class DatasetResponseVM {
    private Integer datasetId;
    private String name;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private Integer rowCount;
    private String columnsDesc;
    private String remark;
    private Integer uploaderId;
    private String uploaderName;
    private String createTime;
}
