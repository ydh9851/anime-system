package com.alvis.media.repository;

import com.alvis.media.domain.UserVideoOperation;
import com.alvis.media.domain.UserVideoOperationKey;
import com.alvis.media.domain.other.KeyValue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserVideoOperationMapper {
    int deleteByPrimaryKey(UserVideoOperationKey key);

    int insert(UserVideoOperation record);

    int insertSelective(UserVideoOperation record);

    UserVideoOperation selectByPrimaryKey(UserVideoOperationKey key);

    int updateByPrimaryKeySelective(UserVideoOperation record);

    int updateByPrimaryKey(UserVideoOperation record);

    List<KeyValue> selectRatingDistribution();
    List<KeyValue> selectActiveUsers();
    List<KeyValue> selectRatingCountDistribution();
    List<KeyValue> selectUserAvgRatingDistribution();
    Map<String, Object> selectUserStats();

    @Select("SELECT * FROM t_user_video_operation WHERE id = #{userId} AND collection = 1")
    List<UserVideoOperation> selectCollections(Integer userId);
}