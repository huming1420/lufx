package com.lufax.dashboard.config;

import com.lufax.dashboard.llm.ExternalLlmAdapter;
import com.lufax.dashboard.llm.LlmAdapter;
import com.lufax.dashboard.llm.MockLlmAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfig {

    @Value("${llm.adapter:mock}")
    private String adapter;

    @Value("${llm.api.key:}")
    private String apiKey;

    @Value("${llm.base.url:}")
    private String baseUrl;

    @Value("${llm.model:}")
    private String model;

    @Value("${llm.protocol:openai}")
    private String protocol;

    @Value("${llm.concurrency:2}")
    private int concurrency;

    @Value("${llm.paic.appId:}")
    private String paicAppId;

    @Value("${llm.paic.botId:}")
    private String paicBotId;

    @Bean
    public LlmAdapter llmAdapter() {
        if ("external".equalsIgnoreCase(adapter)) {
            ExternalLlmAdapter external = new ExternalLlmAdapter();
            external.setApiKey(apiKey);
            external.setBaseUrl(baseUrl);
            external.setModel(model);
            external.setProtocol(protocol);
            external.setConcurrency(concurrency);
            external.setPaicAppId(paicAppId);
            external.setPaicBotId(paicBotId);
            return external;
        }
        return new MockLlmAdapter();
    }
}
