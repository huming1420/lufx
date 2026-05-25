package com.lufax.dashboard.repository;

import com.lufax.dashboard.model.entity.BusinessMetric;
import com.lufax.dashboard.model.entity.CardDefinition;
import com.lufax.dashboard.model.entity.MetricSnapshot;
import com.lufax.dashboard.model.entity.ScenarioBaseline;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BusinessFactMapper {
    int insertSnapshot(MetricSnapshot snapshot);
    int countSnapshots(@Param("period") String period, @Param("metricVersion") String metricVersion);
    MetricSnapshot selectActiveSnapshot(@Param("period") String period);
    int insertMetric(BusinessMetric metric);
    List<BusinessMetric> selectMetrics(@Param("period") String period, @Param("metricVersion") String metricVersion);
    List<BusinessMetric> selectMetricsForCard(@Param("period") String period,
                                              @Param("metricVersion") String metricVersion,
                                              @Param("cardId") String cardId);
    int insertCard(CardDefinition card);
    int insertCardMetric(@Param("cardId") String cardId, @Param("metricCode") String metricCode);
    List<CardDefinition> selectCards();
    int insertScenarioBaseline(ScenarioBaseline baseline);
    List<ScenarioBaseline> selectScenarioBaselines(@Param("period") String period,
                                                    @Param("metricVersion") String metricVersion);
    ScenarioBaseline selectScenarioBaseline(@Param("period") String period,
                                            @Param("metricVersion") String metricVersion,
                                            @Param("scenario") String scenario);
}
