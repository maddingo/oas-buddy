package no.maddin.oasbuddy.core.document;

import java.util.Locale;

public enum DocumentFormat {
    YAML,
    JSON;

    public static DocumentFormat fromFileName(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".json")) {
            return JSON;
        }
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) {
            return YAML;
        }
        throw new IllegalArgumentException("Unrecognized OAS file extension: " + fileName);
    }
}
