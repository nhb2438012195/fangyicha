package com.fangyicha.ai.tool;

import java.util.Map;

/**
 * Thread-local holder for tool execution results.
 * Since ChatClient handles tool calls internally and only returns the final text,
 * this holder captures structured card data from tool executions for the response.
 */
public final class ToolResultHolder {

    private static final ThreadLocal<Map<String, Object>> LAST_RESULT = new ThreadLocal<>();

    private ToolResultHolder() {}

    public static void set(Map<String, Object> result) {
        LAST_RESULT.set(result);
    }

    public static Map<String, Object> get() {
        return LAST_RESULT.get();
    }

    public static void clear() {
        LAST_RESULT.remove();
    }
}
