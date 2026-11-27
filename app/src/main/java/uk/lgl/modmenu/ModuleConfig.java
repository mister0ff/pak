package uk.lgl.modmenu;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small, dependency-free YAML subset loader for flat key: value module files. */
public final class ModuleConfig {
    private final Map<String, String> values = new LinkedHashMap<>();

    private ModuleConfig() {}

    public static ModuleConfig load(Context context, String fileName) {
        ModuleConfig config = new ModuleConfig();
        try (InputStream in = context.getAssets().open("mods/" + fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int split = trimmed.indexOf(':');
                if (split <= 0) continue;
                String key = trimmed.substring(0, split).trim();
                String value = trimmed.substring(split + 1).trim();
                if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2)
                    value = value.substring(1, value.length() - 1);
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2)
                    value = value.substring(1, value.length() - 1);
                config.values.put(key, value);
            }
        } catch (Exception ignored) {
            // Missing/invalid optional config falls back to safe defaults.
        }
        return config;
    }

    public String get(String key, String fallback) {
        String value = values.get(key);
        return value == null ? fallback : value;
    }

    public boolean getBoolean(String key, boolean fallback) {
        String value = values.get(key);
        return value == null ? fallback : "true".equalsIgnoreCase(value);
    }

    public int getInt(String key, int fallback) {
        try { return Integer.parseInt(values.get(key)); }
        catch (Exception ignored) { return fallback; }
    }

    public float getFloat(String key, float fallback) {
        try { return Float.parseFloat(values.get(key)); }
        catch (Exception ignored) { return fallback; }
    }
}
