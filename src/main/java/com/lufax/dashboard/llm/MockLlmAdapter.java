package com.lufax.dashboard.llm;

import com.lufax.dashboard.util.JsonUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deterministic local adapter. It emulates the structured AI contract while
 * external mode sends the same fact pack and prompt to the configured model.
 */
public class MockLlmAdapter implements LlmAdapter {

    @Override
    public String generate(String prompt) {
        return generate(prompt, new LinkedHashMap<String, Object>());
    }

    @Override
    public String generate(String prompt, Map<String, Object> params) {
        String type = String.valueOf(params.get("pack_type"));
        Map<String, Object> response = new LinkedHashMap<>();
        if ("card".equals(type)) {
            String cardId = String.valueOf(params.get("card_id"));
            String light = "risk_analysis".equals(cardId) ? "red" : "yellow";
            response.put("summary", "模型已基于业务指标完成卡片诊断");
            response.put("analysis", "分析依据来自数据库事实包中的指标表现、预算偏差及阈值距离。");
            response.put("recommendation", "持续跟踪关键指标，并对越线风险制定处置动作。");
            response.put("traffic_light", light);
            response.put("analysis_labels", Arrays.asList("AI生成", "指标诊断"));
        } else if ("scenario".equals(type)) {
            String scenario = String.valueOf(params.get("scenario"));
            String light = "bear".equals(scenario) ? "red" : ("bull".equals(scenario) ? "green" : "yellow");
            response.put("scenario", scenario);
            response.put("scenario_label", params.get("scenario_label"));
            response.put("inputs", params.get("inputs"));
            response.put("projected_net_profit", "bear".equals(scenario) ? -18.2 : ("bull".equals(scenario) ? -4.8 : -12.3));
            response.put("standard_explanation", "模型结合当前经营事实与该情景输入估算利润表现。");
            response.put("scenario_exploration", Arrays.asList("关注风险成本变化", "关注规模与定价联动"));
            response.put("traffic_light", light);
            response.put("analysis_labels", Arrays.asList("AI生成", "经营测算"));
        } else {
            response.put("executive_summary", "模型判断当前经营状态承压，需优先关注利润与风险指标。");
            response.put("health_label", "承压");
            response.put("overall_traffic_light", "red");
            response.put("dimensions", Arrays.asList("利润", "风险", "规模"));
            response.put("root_causes", Arrays.asList("利润偏离预算", "风险成本接近阈值"));
            response.put("management_questions", Arrays.asList("风险成本改善节奏是否达标"));
            response.put("analysis_labels", Arrays.asList("AI生成", "经营总览"));
        }
        return JsonUtils.toJson(response);
    }
}
