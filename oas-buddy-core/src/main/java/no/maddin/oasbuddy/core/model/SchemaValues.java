package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Turns what a user typed into a value for {@code enum}, {@code default} or {@code example}, and
 * back.
 *
 * <p>One rule for all three: the text is read as JSON when that yields a value of the schema's
 * type — {@code 42} for an {@code integer}, {@code true} for a {@code boolean}, {@code {"a": 1}}
 * for an {@code object} — so it is written as that type rather than as a quoted string. A
 * {@code string} schema takes the text literally, and anything else that does not parse to the
 * schema's type falls back to a literal string: the editor never refuses input, it lets validation
 * report a value that does not fit.
 */
public final class SchemaValues {

    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private SchemaValues() {
    }

    /** @return the value, or {@code null} for blank text */
    public static JsonNode parse(String text, String type) {
        if (text == null || text.isBlank()) {
            return null;
        }
        if ("string".equals(type)) {
            return TextNode.valueOf(text);
        }
        JsonNode parsed = readJson(text.strip());
        return parsed != null && fits(parsed, type) ? parsed : TextNode.valueOf(text);
    }

    /** How a value is shown for editing: a string as itself, anything else as JSON. */
    public static String display(JsonNode value) {
        if (value == null) {
            return "";
        }
        return value.isTextual() ? value.asText() : value.toString();
    }

    private static JsonNode readJson(String text) {
        try {
            JsonNode node = JSON.readTree(text);
            return node == null || node.isMissingNode() ? null : node;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static boolean fits(JsonNode value, String type) {
        if (type == null) {
            return true;
        }
        return switch (type) {
            case "integer" -> value.isIntegralNumber();
            case "number" -> value.isNumber();
            case "boolean" -> value.isBoolean();
            case "object" -> value.isObject();
            case "array" -> value.isArray();
            default -> false;
        };
    }
}
