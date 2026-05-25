package com.lufax.dashboard.repository;

import com.lufax.dashboard.model.entity.InsightJob;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InsightJobMapper {

    int insert(InsightJob job);

    int updateStatus(@Param("jobId") String jobId,
                     @Param("status") String status,
                     @Param("finishedCount") Integer finishedCount,
                     @Param("failedCount") Integer failedCount,
                     @Param("errorMessage") String errorMessage,
                     @Param("startedAt") String startedAt,
                     @Param("finishedAt") String finishedAt);

    int updateStatusById(@Param("id") Long id, @Param("status") String status);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    InsightJob selectById(@Param("jobId") String jobId);

    InsightJob selectByPrimaryId(@Param("id") Long id);

    List<InsightJob> selectActiveByPeriod(@Param("period") String period);

    List<InsightJob> selectWithPagination(@Param("offset") int offset, @Param("limit") int limit);

    int deleteById(@Param("id") Long id);
}