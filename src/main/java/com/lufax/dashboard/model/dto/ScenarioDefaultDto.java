package com.lufax.dashboard.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScenarioDefaultDto {

    private String scenario;

    @JsonProperty("scenario_label")
    private String scenarioLabel;

    private Double anr;

    @JsonProperty("consumer_finance_growth")
    private Double consumerFinanceGrowth;

    @JsonProperty("pricing_rate")
    private Double pricingRate;

    @JsonProperty("credit_loss_rate")
    private Double creditLossRate;

    @JsonProperty("npl_rate")
    private Double nplRate;

    @JsonProperty("funding_cost_rate")
    private Double fundingCostRate;

    @JsonProperty("sales_cost_rate")
    private Double salesCostRate;

    @JsonProperty("opex_rate")
    private Double opexRate;

    @JsonProperty("tax_other_rate")
    private Double taxOtherRate;

    @JsonProperty("non_loan_profit")
    private Double nonLoanProfit;

    private Double defaultScore;

    @JsonProperty("default_offset_factor")
    private Double defaultOffsetFactor;

    @JsonProperty("default_insight")
    private String defaultInsight;

    public ScenarioDefaultDto() {
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public String getScenarioLabel() {
        return scenarioLabel;
    }

    public void setScenarioLabel(String scenarioLabel) {
        this.scenarioLabel = scenarioLabel;
    }

    public Double getAnr() {
        return anr;
    }

    public void setAnr(Double anr) {
        this.anr = anr;
    }

    public Double getConsumerFinanceGrowth() {
        return consumerFinanceGrowth;
    }

    public void setConsumerFinanceGrowth(Double consumerFinanceGrowth) {
        this.consumerFinanceGrowth = consumerFinanceGrowth;
    }

    public Double getPricingRate() {
        return pricingRate;
    }

    public void setPricingRate(Double pricingRate) {
        this.pricingRate = pricingRate;
    }

    public Double getCreditLossRate() {
        return creditLossRate;
    }

    public void setCreditLossRate(Double creditLossRate) {
        this.creditLossRate = creditLossRate;
    }

    public Double getNplRate() {
        return nplRate;
    }

    public void setNplRate(Double nplRate) {
        this.nplRate = nplRate;
    }

    public Double getFundingCostRate() {
        return fundingCostRate;
    }

    public void setFundingCostRate(Double fundingCostRate) {
        this.fundingCostRate = fundingCostRate;
    }

    public Double getSalesCostRate() {
        return salesCostRate;
    }

    public void setSalesCostRate(Double salesCostRate) {
        this.salesCostRate = salesCostRate;
    }

    public Double getOpexRate() {
        return opexRate;
    }

    public void setOpexRate(Double opexRate) {
        this.opexRate = opexRate;
    }

    public Double getTaxOtherRate() {
        return taxOtherRate;
    }

    public void setTaxOtherRate(Double taxOtherRate) {
        this.taxOtherRate = taxOtherRate;
    }

    public Double getNonLoanProfit() {
        return nonLoanProfit;
    }

    public void setNonLoanProfit(Double nonLoanProfit) {
        this.nonLoanProfit = nonLoanProfit;
    }

    public Double getDefaultScore() {
        return defaultScore;
    }

    public void setDefaultScore(Double defaultScore) {
        this.defaultScore = defaultScore;
    }

    public Double getDefaultOffsetFactor() {
        return defaultOffsetFactor;
    }

    public void setDefaultOffsetFactor(Double defaultOffsetFactor) {
        this.defaultOffsetFactor = defaultOffsetFactor;
    }

    public String getDefaultInsight() {
        return defaultInsight;
    }

    public void setDefaultInsight(String defaultInsight) {
        this.defaultInsight = defaultInsight;
    }
}