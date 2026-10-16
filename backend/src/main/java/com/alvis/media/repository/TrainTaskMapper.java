package com.alvis.media.repository;

import com.alvis.media.domain.TrainTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TrainTaskMapper {

    int insertSelective(TrainTask record);

    TrainTask selectByPrimaryKey(@Param("taskId") Long taskId);

    int updateByPrimaryKeySelective(TrainTask record);

    List<TrainTask> selectAll();

    /** 统计各状态任务数（首页概览用） */
    List<java.util.Map<String, Object>> countGroupByStatus();
}
