package com.lufax.dashboard.repository;

import com.lufax.dashboard.model.entity.InsightJobItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InsightJobItemMapper {

    int insert(InsightJobItem item);

    int update(InsightJobItem item);

    int updateStatus(@Param("id") Integer id,
                     @Param("status") String status);

    int updateStatusFull(@Param("id") Integer id,
                         @Param("status") String status,
                         @Param("resultId") Integer resultId,
                         @Param("errorMessage") String errorMessage);

    List<InsightJobItem> selectByJobId(@Param("jobId") String jobId);

    List<InsightJobItem> selectPendingByJobId(@Param("jobId") String jobId);

    int deleteByJobId(@Param("jobId") Long jobId);
}