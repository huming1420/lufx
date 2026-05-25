package com.lufax.dashboard.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ValidationUtils {

    private static final Logger logger = LoggerFactory.getLogger(ValidationUtils.class);
    private static final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static boolean validateAgainstSchema(String jsonStr, String schemaPath) {
        Map<String, Object> result = validateWithDetails(jsonStr, schemaPath);
        return (Boolean) result.get("valid");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> validateWithDetails(String jsonStr, String schemaPath) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("valid", false);
        result.put("errors", new java.util.ArrayList<Object>());

        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            result.put("errors", java.util.Collections.singletonList("JSON string is empty"));
            return result;
        }

        try {
            String schemaContent = ResourceUtils.readClasspathFileUtf8(schemaPath);
            if (schemaContent.isEmpty()) {
                logger.warn("Schema file not found: {}", schemaPath);
                return result;
            }

            JsonSchema schema = schemaFactory.getSchema(schemaContent);
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(jsonStr);

            Set<ValidationMessage> errors = schema.validate(jsonNode);
            if (errors.isEmpty()) {
                result.put("valid", true);
            } else {
                List<Object> errorList = new java.util.ArrayList<Object>();
                for (ValidationMessage error : errors) {
                    errorList.add(error.getMessage());
                }
                result.put("errors", errorList);
            }
        } catch (Exception e) {
            logger.error("Schema validation error", e);
            result.put("errors", java.util.Collections.singletonList(e.getMessage()));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public static boolean fallbackValidate(String jsonStr, String insightType) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return false;
        }

        try {
            Map<String, Object> json = JsonUtils.toMap(jsonStr);
            if ("card".equals(insightType)) {
                return json.containsKey("summary") && json.containsKey("analysis") && json.containsKey("recommendation");
            } else if ("dashboard".equals(insightType)) {
                return json.containsKey("executive_summary") && json.containsKey("dimensions");
            } else if ("scenario".equals(insightType)) {
                return json.containsKey("scenario_label") && json.containsKey("projected_net_profit");
            }
        } catch (Exception e) {
            logger.warn("Fallback validation error", e);
        }
        return false;
    }

    public static boolean validate(Map<String, Object> data, String schemaContent) {
        if (data == null || schemaContent == null || schemaContent.isEmpty()) {
            return false;
        }
        try {
            String jsonStr = JsonUtils.toJson(data);
            Map<String, Object> result = validateWithDetails(jsonStr, null);
            return (Boolean) result.get("valid");
        } catch (Exception e) {
            logger.error("Validation error", e);
            return false;
        }
    }

    public static Map<String, Object> fallbackValidation(Map<String, Object> data) {
        return data;
    }
}