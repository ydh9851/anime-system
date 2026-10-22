package com.alvis.media.service.impl;

import com.alvis.media.base.SystemCode;
import com.alvis.media.domain.Dataset;
import com.alvis.media.exception.BusinessException;
import com.alvis.media.repository.DatasetMapper;
import com.alvis.media.repository.UserMapper;
import com.alvis.media.service.DatasetService;
import com.alvis.media.utility.DateTimeUtil;
import com.alvis.media.viewmodel.dataset.DatasetResponseVM;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DatasetServiceImpl implements DatasetService {

    private static final Logger logger = LoggerFactory.getLogger(DatasetServiceImpl.class);
    private static final String ALLOWED_EXT = ".csv";
    private static final int MAX_COLUMN_DESC_LEN = 500;

    private final DatasetMapper datasetMapper;
    private final UserMapper userMapper;

    @Value("${system.dataset.dir:../data/dataset}")
    private String datasetDir;

    @Autowired
    public DatasetServiceImpl(DatasetMapper datasetMapper, UserMapper userMapper) {
        this.datasetMapper = datasetMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DatasetResponseVM upload(MultipartFile file, String name, String remark, Integer uploaderId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的数据集文件");
        }
        String originalName = file.getOriginalFilename();
        String lower = originalName == null ? "" : originalName.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(ALLOWED_EXT)) {
            throw new IllegalArgumentException("仅支持 .csv 数据集文件");
        }
        try {
            Path dir = Paths.get(datasetDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            // 重构文件名，避免同名覆盖与路径穿越
            String storeName = UUID.randomUUID().toString().replace("-", "") + ALLOWED_EXT;
            Path target = dir.resolve(storeName).normalize();
            if (!target.startsWith(dir)) {
                throw new IllegalArgumentException("非法文件名");
            }
            // 注意：Undertow 下 MultipartFile.transferTo(File) 会因临时文件被提前清理而抛
            // NoSuchFileException，这里统一用流拷贝落盘，Tomcat/Undertow 行为一致
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            CsvMeta meta = readCsvMeta(target);
            Dataset dataset = new Dataset();
            dataset.setName(StringUtils.isBlank(name) ? originalName : name.trim());
            dataset.setFileName(originalName);
            dataset.setFilePath(storeName);
            dataset.setFileSize(file.getSize());
            dataset.setRowCount(meta.rows);
            dataset.setColumnsDesc(meta.columns);
            dataset.setRemark(remark);
            dataset.setUploaderId(uploaderId);
            dataset.setCreateTime(new Date());
            datasetMapper.insertSelective(dataset);
            return toVM(dataset);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            logger.error("数据集上传失败, filename={}", originalName, e);
            throw new IllegalStateException("数据集上传失败");
        }
    }

    @Override
    public List<DatasetResponseVM> list() {
        List<DatasetResponseVM> result = new ArrayList<>();
        for (Dataset d : datasetMapper.selectAll()) {
            result.add(toVM(d));
        }
        return result;
    }

    @Override
    public DatasetResponseVM detail(Integer datasetId) {
        Dataset dataset = datasetMapper.selectByPrimaryKey(datasetId);
        return dataset == null ? null : toVM(dataset);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DatasetResponseVM update(Integer datasetId, String name, String remark) {
        Dataset dataset = datasetMapper.selectByPrimaryKey(datasetId);
        if (dataset == null) {
            return null;
        }
        Dataset patch = new Dataset();
        patch.setDatasetId(datasetId);
        if (StringUtils.isNotBlank(name)) {
            patch.setName(name.trim());
        }
        if (remark != null) {
            patch.setRemark(remark.trim());
        }
        // 名称与备注都没传时，MyBatis 的动态 SQL 不会生成 SET 子句，
        // 直接执行会得到 "update t_dataset where dataset_id = ?" 的非法语句，
        // 这里提前拦截并给出可读提示。
        if (StringUtils.isBlank(name) && remark == null) {
            throw new BusinessException(SystemCode.ParameterValidError.getCode(), "名称与备注至少需要提供一项");
        }
        datasetMapper.updateByPrimaryKeySelective(patch);
        return detail(datasetId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Integer datasetId) {
        Dataset dataset = datasetMapper.selectByPrimaryKey(datasetId);
        if (dataset == null) {
            return false;
        }
        try {
            Path dir = Paths.get(datasetDir).toAbsolutePath().normalize();
            Path target = dir.resolve(dataset.getFilePath()).normalize();
            if (target.startsWith(dir)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            // 文件删不掉不阻断元数据删除，仅告警
            logger.warn("数据集文件删除失败: {}", dataset.getFilePath(), e);
        }
        return datasetMapper.deleteByPrimaryKey(datasetId) > 0;
    }

    @Override
    public String resolveFilePath(Integer datasetId) {
        if (datasetId == null) {
            return null;
        }
        Dataset dataset = datasetMapper.selectByPrimaryKey(datasetId);
        if (dataset == null) {
            return null;
        }
        return Paths.get(datasetDir).toAbsolutePath().normalize()
                .resolve(dataset.getFilePath()).normalize().toString();
    }

    private DatasetResponseVM toVM(Dataset d) {
        DatasetResponseVM vm = new DatasetResponseVM();
        vm.setDatasetId(d.getDatasetId());
        vm.setName(d.getName());
        vm.setFileName(d.getFileName());
        vm.setFilePath(d.getFilePath());
        vm.setFileSize(d.getFileSize());
        vm.setRowCount(d.getRowCount());
        vm.setColumnsDesc(d.getColumnsDesc());
        vm.setRemark(d.getRemark());
        vm.setUploaderId(d.getUploaderId());
        vm.setCreateTime(DateTimeUtil.dateFormat(d.getCreateTime()));
        if (d.getUploaderId() != null) {
            com.alvis.media.domain.User u = userMapper.selectByPrimaryKey(d.getUploaderId());
            if (u != null) {
                vm.setUploaderName(u.getUserName());
            }
        }
        return vm;
    }

    /** 读 CSV 表头与行数，避免把整个文件读进内存 */
    private CsvMeta readCsvMeta(Path path) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8))) {
            String header = br.readLine();
            // Excel 导出的 CSV 常带 UTF-8 BOM，不剥离会让第一个列名变成 "﻿id"
            if (header != null && !header.isEmpty() && header.charAt(0) == '\uFEFF') {
                header = header.substring(1);
            }
            int rows = 0;
            while (br.readLine() != null) {
                rows++;
            }
            CsvMeta meta = new CsvMeta();
            meta.rows = rows;
            if (header != null && header.length() > MAX_COLUMN_DESC_LEN) {
                header = header.substring(0, MAX_COLUMN_DESC_LEN);
            }
            meta.columns = header;
            return meta;
        }
    }

    private static class CsvMeta {
        int rows;
        String columns;
    }
}
