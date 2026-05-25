package com.lufax.dashboard.llm;

import java.util.Map;

public interface LlmAdapter {

    String generate(String prompt);

    String generate(String prompt, Map<String, Object> params);
}