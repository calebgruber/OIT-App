package com.oitapp.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ResponseHelper {
    private ResponseHelper() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Map<String, Object> source, String key) {
        if (source == null || key == null) {
            return Collections.emptyMap();
        }
        Object value = source.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getList(Map<String, Object> source, String key) {
        if (source == null || key == null) {
            return Collections.emptyList();
        }
        Object value = source.get(key);
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<?> rawList = (List<?>) value;
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof Map) {
                mapped.add((Map<String, Object>) item);
            }
        }
        return mapped;
    }

    public static Map<String, Object> unwrapDataMap(Map<String, Object> response, String preferredKey) {
        Map<String, Object> specific = getMap(response, preferredKey);
        if (!specific.isEmpty()) {
            return specific;
        }
        Map<String, Object> data = getMap(response, "data");
        if (!data.isEmpty()) {
            Map<String, Object> nested = getMap(data, preferredKey);
            return nested.isEmpty() ? data : nested;
        }
        return response == null ? Collections.<String, Object>emptyMap() : response;
    }

    public static List<Map<String, Object>> unwrapDataList(Map<String, Object> response, String preferredKey) {
        List<Map<String, Object>> direct = getList(response, preferredKey);
        if (!direct.isEmpty()) {
            return direct;
        }
        Map<String, Object> data = getMap(response, "data");
        List<Map<String, Object>> nested = getList(data, preferredKey);
        if (!nested.isEmpty()) {
            return nested;
        }
        List<Map<String, Object>> dataList = getList(response, "data");
        if (!dataList.isEmpty()) {
            return dataList;
        }
        return Collections.emptyList();
    }

    public static String getString(Map<String, Object> source, String key) {
        if (source == null || key == null) {
            return "";
        }
        Object value = source.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    public static int getInt(Map<String, Object> source, String key) {
        if (source == null || key == null) {
            return 0;
        }
        Object value = source.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    public static boolean getBoolean(Map<String, Object> source, String key) {
        if (source == null || key == null) {
            return false;
        }
        Object value = source.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }
}
