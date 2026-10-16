package com.alvis.media.service;

import com.alvis.media.viewmodel.dataset.DatasetResponseVM;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 数据集管理Service：上传 / 列表 / 详情 / 删除
 */
public interface DatasetService {

    /**
     * 上传数据集（仅接受 .csv）
     *
     * @param file       上传文件
     * @param name       数据集名称，为空时取原始文件名
     * @param remark     备注
     * @param uploaderId 上传人 id
     * @return 落库后的数据集
     */
    DatasetResponseVM upload(MultipartFile file, String name, String remark, Integer uploaderId);

    /**
     * 查询全部数据集
     */
    List<DatasetResponseVM> list();

    /**
     * 数据集详情
     *
     * @param datasetId 数据集 id
     * @return DatasetResponseVM，不存在时返回 null
     */
    DatasetResponseVM detail(Integer datasetId);

    /**
     * 修改数据集名称 / 备注
     *
     * @param datasetId 数据集 id
     * @param name      新名称，为空表示不修改
     * @param remark    新备注，为空表示不修改
     * @return 修改后的数据集；不存在时返回 null
     */
    DatasetResponseVM update(Integer datasetId, String name, String remark);

    /**
     * 删除数据集（同时删除磁盘文件）
     *
     * @param datasetId 数据集 id
     * @return true 删除成功；false 表示记录不存在
     */
    boolean delete(Integer datasetId);

    /**
     * 获取数据集在磁盘上的绝对路径，供训练任务使用
     *
     * @param datasetId 数据集 id
     * @return 绝对路径；不存在返回 null
     */
    String resolveFilePath(Integer datasetId);
}
