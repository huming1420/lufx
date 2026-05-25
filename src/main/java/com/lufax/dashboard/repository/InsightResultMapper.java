package com.lufax.dashboard.repository;

import com.lufax.dashboard.model.entity.InsightResult;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InsightResultMapper {

    int insertOrUpdate(InsightResult record);

    InsightResult selectByCacheKey(@Param("period") String period,
                                   @Param("metricVersion") String metricVersion,
                                   @Param("promptVersion") String promptVersion,
                                   @Param("insightType") String insightType,
                                   @Param("cardId") String cardId);

    InsightResult selectById(@Param("id") Long id);

    List<InsightResult> selectMatchingScenarioCache(@Param("period") String period,
                                                    @Param("metricVersion") String metricVersion,
                                                    @Param("promptVersion") String promptVersion);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("errorMessage") String errorMessage);

    List<InsightResult> selectByPeriodAndType(@Param("period") String period,
                                              @Param("metricVersion") String metricVersion,
                                              @Param("promptVersion") String promptVersion,
                                              @Param("insightType") String insightType);

    InsightResult selectByScenarioHash(@Param("scenarioHash") String scenarioHash);

    List<InsightResult> selectByScenarioAndDate(@Param("scenario") String scenario,
                                                @Param("metricDate") String metricDate);

    List<InsightResult> selectByScenario(@Param("scenario") String scenario);

    int upsert(@Param("record") InsightResult record);

    InsightResult selectByScenarioAndHash(@Param("scenario") String scenario,
                                          @Param("hash") String hash);
}