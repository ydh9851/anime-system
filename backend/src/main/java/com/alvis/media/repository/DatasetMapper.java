package com.alvis.media.repository;

import com.alvis.media.domain.Dataset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DatasetMapper {

    int insertSelective(Dataset record);

    Dataset selectByPrimaryKey(@Param("datasetId") Integer datasetId);

    int updateByPrimaryKeySelective(Dataset record);

    int deleteByPrimaryKey(@Param("datasetId") Integer datasetId);

    List<Dataset> selectAll();

    Dataset selectByFilePath(@Param("filePath") String filePath);
}
