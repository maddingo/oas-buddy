package no.maddin.oasbuddy.core.model;

import java.util.List;

/**
 * The single statement of which {@code style} a parameter may use for each {@code in}, what a
 * missing {@code style} means, and what {@code explode} defaults to — the same job {@link Constraint}
 * does for schema keywords. The editor and anything that checks a document both read it.
 *
 * <p>OAS 3.0: path takes {@code matrix}/{@code label}/{@code simple} (default {@code simple}), query
 * takes {@code form}/{@code spaceDelimited}/{@code pipeDelimited}/{@code deepObject} (default
 * {@code form}), header only {@code simple}, cookie only {@code form}. {@code explode} defaults to
 * {@code true} for {@code form} and {@code false} for everything else.
 */
public final class ParameterStyles {

    private ParameterStyles() {
    }

    /** The styles valid for a location; empty for a location this class does not know. */
    public static List<String> validFor(String in) {
        if (in == null) {
            return List.of();
        }
        return switch (in) {
            case "path" -> List.of("matrix", "label", "simple");
            case "query" -> List.of("form", "spaceDelimited", "pipeDelimited", "deepObject");
            case "header" -> List.of("simple");
            case "cookie" -> List.of("form");
            default -> List.of();
        };
    }

    /** What an absent {@code style} means for a location, or {@code null} for an unknown one. */
    public static String defaultFor(String in) {
        List<String> valid = validFor(in);
        if (valid.isEmpty()) {
            return null;
        }
        return "path".equals(in) || "header".equals(in) ? "simple" : "form";
    }

    /** What an absent {@code explode} means for a style: {@code true} only for {@code form}. */
    public static boolean defaultExplode(String style) {
        return "form".equals(style);
    }

    /** {@code allowReserved} and {@code allowEmptyValue} exist only for query parameters. */
    public static boolean allowsReservedAndEmpty(String in) {
        return "query".equals(in);
    }
}
