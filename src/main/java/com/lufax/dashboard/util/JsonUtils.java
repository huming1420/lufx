package com.lufax.dashboard.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JsonUtils {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObjectMapper sortedMapper = new ObjectMapper();
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("\\{[^{}]*\\}");
    private static final Pattern MARKDOWN_CODE_BLOCK = Pattern.compile("```(?:json)?\\s*(.+?)\\s*```", Pattern.DOTALL);

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        sortedMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        sortedMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        sortedMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        sortedMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> readTree(String json) {
        try {
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new LinkedHashMap<String, Object>();
        }
    }

    public static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    public static String toSortedJson(Object obj) {
        try {
            return sortedMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(String json) {
        try {
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new LinkedHashMap<String, Object>();
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return mapper.readValue(json, typeRef);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static String safeJsonGet(String json, String key) {
        try {
            Map<String, Object> node = mapper.readValue(json, Map.class);
            Object value = node.get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadsJsonObject(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new LinkedHashMap<String, Object>();
        }

        String cleaned = raw.trim();

        Matcher codeBlockMatcher = MARKDOWN_CODE_BLOCK.matcher(cleaned);
        if (codeBlockMatcher.find()) {
            cleaned = codeBlockMatcher.group(1).trim();
        }

        try {
            return mapper.readValue(cleaned, Map.class);
        } catch (Exception e) {
            Matcher jsonMatcher = JSON_BLOCK_PATTERN.matcher(cleaned);
            if (jsonMatcher.find()) {
                try {
                    return mapper.readValue(jsonMatcher.group(), Map.class);
                } catch (Exception ex) {
                    return new LinkedHashMap<String, Object>();
                }
            }
            return new LinkedHashMap<String, Object>();
        }
    }

    public static Object extractFirstString(Object json, List<Object> paths) {
        if (json == null || paths == null || paths.isEmpty()) {
            return null;
        }

        Object current = json;
        for (Object path : paths) {
            if (current == null) {
                return null;
            }
            if (path instanceof Integer && current instanceof List) {
                List<?> list = (List<?>) current;
                int index = (Integer) path;
                if (index >= 0 && index < list.size()) {
                    current = list.get(index);
                } else {
                    return null;
                }
            } else if (path instanceof String && current instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) current;
                current = map.get(path);
            } else {
                return null;
            }
        }
        return current;
    }

    public static String canonicalPayload(Map<String, Object> payload) {
        if (payload == null) {
            return "{}";
        }
        return toSortedJson(payload);
    }

    public static boolean numericWithinTolerance(Object val1, Object val2, double tolerance) {
        if (val1 == null || val2 == null) {
            return val1 == val2;
        }
        double d1 = toDouble(val1);
        double d2 = toDouble(val2);
        return Math.abs(d1 - d2) <= tolerance;
    }

    private static double toDouble(Object val) {
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}