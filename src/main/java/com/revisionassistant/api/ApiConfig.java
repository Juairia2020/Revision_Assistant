package com.revisionassistant.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Reads the AI API credential and model name from outside the source
 * code, so nothing secret is ever committed to Git. Two sources are
 * checked, in order:
 * <ol>
 *   <li>The {@code REVISION_ASSISTANT_API_KEY} / {@code REVISION_ASSISTANT_MODEL}
 *       environment variables.</li>
 *   <li>A {@code config.properties} file in the project's working directory
 *       (git-ignored - copy {@code config.properties.example} to create it),
 *       holding an {@code api.key} and, optionally, an {@code api.model}
 *       property.</li>
 * </ol>
 * If neither source provides a key, {@link #hasApiKey()} simply returns
 * {@code false} - AI generation becomes unavailable, but the rest of
 * the application keeps working normally.
 */
public final class ApiConfig {

    private static final String ENV_KEY = "REVISION_ASSISTANT_API_KEY";
    private static final String ENV_MODEL = "REVISION_ASSISTANT_MODEL";
    private static final String CONFIG_FILE = "config.properties";
    private static final String DEFAULT_MODEL = "claude-haiku-4-5-20251001";

    private ApiConfig() {
        // Utility class - no instances.
    }

    /** True when a (non-blank) API key is available from either source. */
    public static boolean hasApiKey() {
        String key = getApiKey();
        return key != null && !key.isBlank();
    }

    /** The configured API key, or {@code null} if none is set anywhere. */
    public static String getApiKey() {
        String fromEnv = System.getenv(ENV_KEY);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromFile = readProperty("api.key");
        return (fromFile == null || fromFile.isBlank()) ? null : fromFile.trim();
    }

    /** The model to use for generation - a documented default if nothing is configured. */
    public static String getModel() {
        String fromEnv = System.getenv(ENV_MODEL);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromFile = readProperty("api.model");
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }
        return DEFAULT_MODEL;
    }

    private static String readProperty(String key) {
        Path path = Path.of(CONFIG_FILE);
        if (!Files.isRegularFile(path)) {
            return null;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        } catch (IOException e) {
            return null;
        }
        return properties.getProperty(key);
    }
}
