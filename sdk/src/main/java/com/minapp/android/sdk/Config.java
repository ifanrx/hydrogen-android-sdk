package com.minapp.android.sdk;

import androidx.annotation.Nullable;

import com.minapp.android.sdk.util.Util;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局配置
 */
public class Config {
    private Config() {}

    public static final String CLIENT_ID = "CLIENT_ID";
    public static final String ENV_ID = "ENV_ID";

    /**
     * 自定义域名
     */
    public static final String ENDPOINT_KEY = "ENDPOINT_KEY";
    public static final String DEFAULT_ENDPOINT = "https://cloud.minapp.com/";

    private static final Map<String, Object> CONFIG = new HashMap<>();

    public static @Nullable String getClientId() {
        return (String) CONFIG.get(CLIENT_ID);
    }

    public static void setClientId(String clientId) {
        CONFIG.put(CLIENT_ID, clientId);
    }

    public static void setEndpoint(String endpoint) {
        CONFIG.put(ENDPOINT_KEY, endpoint);
    }

    public static @NotNull String getEndpoint() {
        Object value = CONFIG.get(ENDPOINT_KEY);
        if (value instanceof String) {
            String endpoint = (String) value;
            endpoint = Util.trimToNull(endpoint);
            if (endpoint != null)
                return endpoint;
        }
        return DEFAULT_ENDPOINT;
    }

    public static void setEnvId(String envId) {
        CONFIG.put(ENV_ID, envId);
    }

    public static @Nullable String getEnvId() {
        Object value = CONFIG.get(ENV_ID);
        return value != null ? (String) value : null;
    }
}
