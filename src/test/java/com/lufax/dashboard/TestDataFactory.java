package com.lufax.dashboard;

import com.lufax.dashboard.model.dto.*;
import com.lufax.dashboard.model.entity.InsightJob;
import com.lufax.dashboard.model.entity.InsightJobItem;
import com.lufax.dashboard.model.entity.InsightResult;
import com.lufax.dashboard.model.request.CardInsightRequest;
import com.lufax.dashboard.model.request.CreateJobRequest;
import com.lufax.dashboard.model.request.DashboardInsightRequest;
import com.lufax.dashboard.model.request.ScenarioInsightRequest;

import java.util.*;

/**
 * Test data factory - centralized test fixture generation for all test classes.
 * Ensures consistency across tests and reduces boilerplate.
 */
public class TestDataFactory {

    // ========== DashboardInsightRequest ==========
    public static DashboardInsightRequest createDashboardRequest(String scenario) {
        DashboardInsightRequest request = new DashboardInsightRequest();
        request.setScenario(scenario != null ? scenario : "base");
        request.setPeriod("2026-03-YTD");
        request.setMetricDate("2026-03-20");
        request.setInputData(createInputData());
        return request;
    }

    public static DashboardInsightRequest createDashboardRequest() {
        return createDashboardRequest("base");
    }

    // ========== CardInsightRequest ==========
    public static CardInsightRequest createCardRequest(String cardId) {
        CardInsightRequest request = new CardInsightRequest();
        request.setCardId(cardId != null ? cardId : "profit_analysis");
        request.setScenario("base");
        request.setInputData(createInputData());
        return request;
    }

    public static CardInsightRequest createCardRequest() {
        return createCardRequest("profit_analysis");
    }

    // ========== ScenarioInsightRequest ==========
    public static ScenarioInsightRequest createScenarioRequest(String scenario) {
        ScenarioInsightRequest request = new ScenarioInsightRequest();
        request.setScenario(scenario != null ? scenario : "base");
        request.setMetricDate("2026-03-20");
        request.setInputData(createInputData());
        return request;
    }

    public static ScenarioInsightRequest createScenarioRequest() {
        return createScenarioRequest("base");
    }

    // ========== CreateJobRequest ==========
    public static CreateJobRequest createJobRequest(int itemCount) {
        CreateJobRequest request = new CreateJobRequest();
        request.setJobType("scenario_insight");
        request.setPeriod("2026-03-YTD");
        List<CreateJobRequest.JobItemRequest> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            CreateJobRequest.JobItemRequest item = new CreateJobRequest.JobItemRequest();
            item.setScenario(i % 2 == 0 ? "base" : "bear");
            item.setMetricDate("2026-03-20");
            items.add(item);
        }
        request.setItems(items);
        return request;
    }

    // ========== Input Data (common fact payload) ==========
    @SuppressWarnings("unchecked")
    public static Map<String, Object> createInputData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("score", 72.5);
        data.put("offset_factor", 1.05);
        data.put("total_amount", 15000000.0);
        data.put("count", 2500L);
        data.put("avg_amount", 6000.0);
        data.put("metric_date", "2026-03-20");
        data.put("report_type", "monthly");

        // dimensions
        List<Map<String, Object>> dimensions = new ArrayList<>();
        Map<String, Object> dim1 = new LinkedHashMap<>();
        dim1.put("dimension", "profitability");
        dim1.put("score", 85.0);
        dim1.put("contribution", 40.0);
        dim1.put("count", 1000L);
        Map<String, Object> dim2 = new LinkedHashMap<>();
        dim2.put("dimension", "risk");
        dim2.put("score", 60.0);
        dim2.put("contribution", 25.0);
        dim2.put("count", 800L);
        dimensions.add(dim1);
        dimensions.add(dim2);
        data.put("dimensions", dimensions);

        // cross_signals
        List<Map<String, Object>> signals = new ArrayList<>();
        Map<String, Object> sig1 = new LinkedHashMap<>();
        sig1.put("signal_name", "market_volatility");
        sig1.put("score", 75.0);
        sig1.put("trend", "rising");
        sig1.put("description", "Market volatility increasing");
        signals.add(sig1);
        data.put("cross_signals", signals);

        // root_causes
        List<Map<String, Object>> causes = new ArrayList<>();
        Map<String, Object> cause1 = new LinkedHashMap<>();
        cause1.put("dimension", "credit_loss");
        cause1.put("value", "high");
        cause1.put("impact", 30.0);
        cause1.put("confidence", 85.0);
        cause1.put("description", "Credit loss rate above threshold");
        causes.add(cause1);
        data.put("root_causes", causes);

        return data;
    }

    // ========== CoreMetricDto ==========
    public static CoreMetricDto createCoreMetric() {
        CoreMetricDto metric = new CoreMetricDto();
        metric.setScore(72.5);
        metric.setOffsetFactor(1.05);
        metric.setTotalAmount(15000000.0);
        metric.setCount(2500L);
        metric.setAvgAmount(6000.0);
        metric.setMetricDate("2026-03-20");
        metric.setReportType("monthly");
        return metric;
    }

    // ========== DimensionScoreDto list ==========
    public static List<DimensionScoreDto> createDimensionScores() {
        List<DimensionScoreDto> scores = new ArrayList<>();
        DimensionScoreDto d1 = new DimensionScoreDto();
        d1.setDimension("profitability");
        d1.setScore(85.0);
        d1.setContribution(40.0);
        d1.setCount(1000L);
        DimensionScoreDto d2 = new DimensionScoreDto();
        d2.setDimension("risk");
        d2.setScore(60.0);
        d2.setContribution(25.0);
        d2.setCount(800L);
        scores.add(d1);
        scores.add(d2);
        return scores;
    }

    // ========== CrossSignalDto list ==========
    public static List<CrossSignalDto> createCrossSignals() {
        List<CrossSignalDto> signals = new ArrayList<>();
        CrossSignalDto s1 = new CrossSignalDto();
        s1.setSignalName("market_volatility");
        s1.setScore(75.0);
        s1.setTrend("rising");
        s1.setDescription("Market volatility increasing");
        signals.add(s1);
        return signals;
    }

    // ========== RootCauseDto list ==========
    public static List<RootCauseDto> createRootCauses() {
        List<RootCauseDto> causes = new ArrayList<>();
        RootCauseDto c1 = new RootCauseDto();
        c1.setDimension("credit_loss");
        c1.setValue("high");
        c1.setImpact(30.0);
        c1.setConfidence(85.0);
        c1.setDescription("Credit loss rate above threshold");
        causes.add(c1);
        return causes;
    }

    // ========== RulesDto ==========
    public static RulesDto createRules() {
        RulesDto rules = new RulesDto();
        rules.setHealthScore(72.5);
        rules.setScoreThreshold(5.0);

        List<Object> ruleList = new ArrayList<>();
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("name", "high_risk_alert");
        rule.put("score", 3.0);

        List<Map<String, Object>> conditions = new ArrayList<>();
        Map<String, Object> cond = new LinkedHashMap<>();
        cond.put("type", "core_metric");
        cond.put("field", "score");
        cond.put("operator", "<");
        cond.put("value", 60.0);
        conditions.add(cond);
        rule.put("conditions", conditions);

        ruleList.add(rule);
        rules.setRules(ruleList);
        rules.setVersion("1.0");

        rules.setDimensionScores(createDimensionScores());
        rules.setRootCauses(createRootCauses());
        return rules;
    }

    // ========== InsightResult Entity ==========
    public static InsightResult createInsightResult(Long id) {
        InsightResult result = new InsightResult();
        result.setId(id);
        result.setScenario("base");
        result.setMetricDate("2026-03-20");
        result.setScenarioHash("abc123hash");
        result.setInsightJson("{\"summary\":\"test\"}");
        result.setScore(72.5);
        result.setStatus("ready");
        result.setValidated(true);
        result.setPeriod("2026-03-YTD");
        result.setVersion("v1");
        return result;
    }

    // ========== InsightJob Entity ==========
    public static InsightJob createInsightJob(String jobIdStr) {
        InsightJob job = new InsightJob();
        job.setId(jobIdStr != null ? Long.parseLong(jobIdStr) : 1L);
        job.setJobType("scenario_insight");
        job.setStatus("PENDING");
        job.setTotalCount(3);
        job.setFinishedCount(1);
        job.setFailedCount(0);
        job.setPeriod("2026-03-YTD");
        job.setCreatedAt("2026-03-20T10:00:00Z");
        return job;
    }

    public static InsightJob createInsightJob() {
        return createInsightJob("1");
    }

    // ========== InsightJobItem Entity ==========
    public static InsightJobItem createJobItem(Long jobId) {
        InsightJobItem item = new InsightJobItem();
        item.setJobId(jobId);
        item.setScenario("base");
        item.setMetricDate("2026-03-20");
        item.setStatus("PENDING");
        item.setCreatedAt("2026-03-20T10:00:00Z");
        return item;
    }

    // ========== ScenarioDefaultDto ==========
    public static ScenarioDefaultDto createScenarioDefault(String scenario) {
        ScenarioDefaultDto dto = new ScenarioDefaultDto();
        dto.setScenario(scenario != null ? scenario : "base");
        dto.setScenarioLabel(scenario != null ? scenario.toUpperCase() + " Scenario" : "BASE Scenario");
        dto.setDefaultScore(70.0);
        dto.setDefaultOffsetFactor(1.0);
        dto.setDefaultInsight("Normal operations");
        dto.setAnr(3.5);
        dto.setConsumerFinanceGrowth(12.0);
        dto.setPricingRate(8.5);
        dto.setCreditLossRate(2.0);
        dto.setNplRate(1.5);
        dto.setFundingCostRate(3.0);
        dto.setSalesCostRate(4.0);
        dto.setOpexRate(25.0);
        dto.setTaxOtherRate(15.0);
        dto.setNonLoanProfit(5000000.0);
        return dto;
    }
}
