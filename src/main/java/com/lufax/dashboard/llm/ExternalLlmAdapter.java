package com.lufax.dashboard.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class ExternalLlmAdapter implements LlmAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ExternalLlmAdapter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int DEFAULT_TIMEOUT_MS = 120000; // 2 minutes

    private String apiKey;
    private String baseUrl;
    private String model;
    private String protocol;
    private int concurrency;
    private String paicAppId;
    private String paicBotId;

    private Semaphore semaphore;

    public void init() {
        this.semaphore = new Semaphore(concurrency);
    }

    @Override
    public String generate(String prompt) {
        return generate(prompt, null);
    }

    @Override
    public String generate(String prompt, Map<String, Object> params) {
        if (semaphore == null) {
            init();
        }
        try {
            semaphore.acquire();
            return doCall(prompt, params);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("LLM call interrupted", e);
        } finally {
            semaphore.release();
        }
    }

    protected String doCall(String prompt, Map<String, Object> params) {
        if ("paic".equalsIgnoreCase(protocol)) {
            return doPaicCall(prompt, params);
        } else if ("openai".equalsIgnoreCase(protocol)) {
            return doOpenAiCall(prompt, params);
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported LLM protocol: " + protocol);
        }
    }

    /**
     * Call PAIC Agent Gateway (平安内部 Agent 网关)
     * Gateway: http://agents-api-sze.paic.com.cn
     */
    private String doPaicCall(String prompt, Map<String, Object> params) {
        String url = baseUrl + "/api/v1/agent/chat";

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("appId", paicAppId);
            requestBody.put("appKey", apiKey);
            requestBody.put("botId", paicBotId);
            requestBody.put("model", model);

            // Build messages array
            ArrayNode messages = requestBody.putArray("messages");

            // Build user message content: prompt + fact_pack data
            String userContent = buildUserContent(prompt, params);

            ObjectNode userMessage = messages.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", userContent);

            // Optional: add system message if params contains system prompt
            if (params != null && params.containsKey("system")) {
                ObjectNode systemMessage = objectMapper.createObjectNode();
                systemMessage.put("role", "system");
                systemMessage.put("content", String.valueOf(params.get("system")));
                messages.insert(0, systemMessage);
            }

            // Optional parameters
            if (params != null) {
                if (params.containsKey("temperature")) {
                    requestBody.put("temperature", Double.parseDouble(String.valueOf(params.get("temperature"))));
                }
                if (params.containsKey("max_tokens")) {
                    requestBody.put("maxTokens", Integer.parseInt(String.valueOf(params.get("max_tokens"))));
                }
            }

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            logger.debug("PAIC request body: {}", jsonBody);

            String responseJson = executeHttpPost(url, jsonBody, buildPaicHeaders());
            logger.debug("PAIC response: {}", responseJson);

            return extractPaicResponse(responseJson);

        } catch (IOException e) {
            logger.error("PAIC LLM call failed", e);
            throw new RuntimeException("PAIC LLM call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Call OpenAI-compatible API (备用协议)
     */
    private String doOpenAiCall(String prompt, Map<String, Object> params) {
        String url = baseUrl + "/chat/completions";

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);

            ArrayNode messages = requestBody.putArray("messages");

            // Build user message content: prompt + fact_pack data
            String userContent = buildUserContent(prompt, params);

            ObjectNode userMessage = messages.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", userContent);

            if (params != null && params.containsKey("system")) {
                ObjectNode systemMessage = objectMapper.createObjectNode();
                systemMessage.put("role", "system");
                systemMessage.put("content", String.valueOf(params.get("system")));
                messages.insert(0, systemMessage);
            }

            if (params != null) {
                if (params.containsKey("temperature")) {
                    requestBody.put("temperature", Double.parseDouble(String.valueOf(params.get("temperature"))));
                }
                if (params.containsKey("max_tokens")) {
                    requestBody.put("max_tokens", Integer.parseInt(String.valueOf(params.get("max_tokens"))));
                }
            }

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            logger.debug("OpenAI request body: {}", jsonBody);

            String responseJson = executeHttpPost(url, jsonBody, buildOpenAiHeaders());
            logger.debug("OpenAI response: {}", responseJson);

            return extractOpenAiResponse(responseJson);

        } catch (IOException e) {
            logger.error("OpenAI LLM call failed", e);
            throw new RuntimeException("OpenAI LLM call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build user message content by embedding fact_pack data into the prompt.
     * Meta keys (system, temperature, max_tokens) are excluded from fact data
     * as they are LLM configuration, not business data.
     */
    private String buildUserContent(String prompt, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return prompt;
        }

        // Build fact data by excluding meta keys
        Map<String, Object> factData = new LinkedHashMap<>(params);
        factData.remove("system");
        factData.remove("temperature");
        factData.remove("max_tokens");

        if (factData.isEmpty()) {
            return prompt;
        }

        try {
            String factJson = objectMapper.writeValueAsString(factData);
            return prompt + "\n\nFACT_PACK:\n" + factJson;
        } catch (Exception e) {
            logger.warn("Failed to serialize fact_pack data, sending prompt only", e);
            return prompt;
        }
    }

    private String executeHttpPost(String url, String jsonBody, Map<String, String> headers) throws IOException {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(DEFAULT_TIMEOUT_MS)
                .setSocketTimeout(DEFAULT_TIMEOUT_MS)
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json; charset=UTF-8");

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }

            httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (statusCode < 200 || statusCode >= 300) {
                    logger.error("HTTP error {}: {}", statusCode, responseBody);
                    throw new RuntimeException("HTTP " + statusCode + ": " + responseBody);
                }

                return responseBody;
            }
        }
    }

    private Map<String, String> buildPaicHeaders() {
        Map<String, String> headers = new java.util.HashMap<>();
        headers.put("X-App-Id", paicAppId);
        headers.put("X-App-Key", apiKey);
        return headers;
    }

    private Map<String, String> buildOpenAiHeaders() {
        Map<String, String> headers = new java.util.HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        return headers;
    }

    private String extractPaicResponse(String responseJson) throws IOException {
        JsonNode root = objectMapper.readTree(responseJson);

        // Check for error
        if (root.has("error")) {
            JsonNode error = root.get("error");
            String errorMsg = error.has("message") ? error.get("message").asText() : error.toString();
            throw new RuntimeException("PAIC API error: " + errorMsg);
        }

        // Try common response formats
        // Format 1: { "data": { "content": "..." } }
        if (root.has("data") && root.get("data").has("content")) {
            return root.get("data").get("content").asText();
        }

        // Format 2: { "choices": [{ "message": { "content": "..." } }] }
        if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
            JsonNode firstChoice = root.get("choices").get(0);
            if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                return firstChoice.get("message").get("content").asText();
            }
            if (firstChoice.has("text")) {
                return firstChoice.get("text").asText();
            }
        }

        // Format 3: { "result": "..." }
        if (root.has("result")) {
            return root.get("result").asText();
        }

        // Format 4: { "response": "..." }
        if (root.has("response")) {
            return root.get("response").asText();
        }

        // Fallback: return the whole response for debugging
        logger.warn("Unexpected PAIC response format: {}", responseJson);
        return responseJson;
    }

    private String extractOpenAiResponse(String responseJson) throws IOException {
        JsonNode root = objectMapper.readTree(responseJson);

        if (root.has("error")) {
            JsonNode error = root.get("error");
            String errorMsg = error.has("message") ? error.get("message").asText() : error.toString();
            throw new RuntimeException("OpenAI API error: " + errorMsg);
        }

        if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
            JsonNode firstChoice = root.get("choices").get(0);
            if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                return firstChoice.get("message").get("content").asText();
            }
        }

        logger.warn("Unexpected OpenAI response format: {}", responseJson);
        return responseJson;
    }

    // Setters
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public void setPaicAppId(String paicAppId) {
        this.paicAppId = paicAppId;
    }

    public void setPaicBotId(String paicBotId) {
        this.paicBotId = paicBotId;
    }
}
