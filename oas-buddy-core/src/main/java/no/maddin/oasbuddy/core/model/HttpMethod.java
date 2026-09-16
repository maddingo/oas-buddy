package no.maddin.oasbuddy.core.model;

import java.util.Locale;

public enum HttpMethod {
    GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE;

    public String fieldName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
