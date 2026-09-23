package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * The JSON-Schema validation keywords OAS 3.0 keeps, each with the kind of value it takes and the
 * types it constrains. The one place that says so: the editor shows a constraint only for its own
 * types, and {@code validation.ConstraintCheck} checks values against the same kinds.
 *
 * <p>{@code exclusiveMinimum}/{@code exclusiveMaximum} are booleans modifying {@code minimum}/
 * {@code maximum} here — OAS 3.0 follows JSON Schema draft 4, not the numeric form of later drafts.
 */
public enum Constraint {
    MINIMUM("minimum", "Minimum", Kind.NUMBER, Group.NUMERIC),
    EXCLUSIVE_MINIMUM("exclusiveMinimum", "Exclusive minimum", Kind.BOOLEAN, Group.NUMERIC),
    MAXIMUM("maximum", "Maximum", Kind.NUMBER, Group.NUMERIC),
    EXCLUSIVE_MAXIMUM("exclusiveMaximum", "Exclusive maximum", Kind.BOOLEAN, Group.NUMERIC),
    MULTIPLE_OF("multipleOf", "Multiple of", Kind.NUMBER, Group.NUMERIC),
    MIN_LENGTH("minLength", "Min length", Kind.COUNT, Group.STRING),
    MAX_LENGTH("maxLength", "Max length", Kind.COUNT, Group.STRING),
    PATTERN("pattern", "Pattern", Kind.TEXT, Group.STRING),
    MIN_ITEMS("minItems", "Min items", Kind.COUNT, Group.ARRAY),
    MAX_ITEMS("maxItems", "Max items", Kind.COUNT, Group.ARRAY),
    UNIQUE_ITEMS("uniqueItems", "Unique items", Kind.BOOLEAN, Group.ARRAY),
    MIN_PROPERTIES("minProperties", "Min properties", Kind.COUNT, Group.OBJECT),
    MAX_PROPERTIES("maxProperties", "Max properties", Kind.COUNT, Group.OBJECT);

    /** What a constraint's value is. */
    public enum Kind {
        /** Any number. */
        NUMBER,
        /** A non-negative integer. */
        COUNT,
        /** Only ever written as {@code true}; {@code false} is the default and is left out. */
        BOOLEAN,
        TEXT
    }

    private enum Group {
        NUMERIC("integer", "number"),
        STRING("string"),
        ARRAY("array"),
        OBJECT("object");

        private final Set<String> types;

        Group(String... types) {
            this.types = Set.of(types);
        }
    }

    /** Lower/upper bound pairs, where the lower one above the upper one admits no value at all. */
    public static final List<List<Constraint>> RANGES = List.of(
            List.of(MINIMUM, MAXIMUM),
            List.of(MIN_LENGTH, MAX_LENGTH),
            List.of(MIN_ITEMS, MAX_ITEMS),
            List.of(MIN_PROPERTIES, MAX_PROPERTIES));

    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private final String key;
    private final String label;
    private final Kind kind;
    private final Group group;

    Constraint(String key, String label, Kind kind, Group group) {
        this.key = key;
        this.label = label;
        this.kind = kind;
        this.group = group;
    }

    /** The constraints that apply to a type, in display order; none for a missing or other type. */
    public static List<Constraint> forType(String type) {
        return Arrays.stream(values()).filter(c -> c.appliesTo(type)).toList();
    }

    public boolean appliesTo(String type) {
        return type != null && group.types.contains(type);
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public Kind kind() {
        return kind;
    }

    /** Whether a value in the document is of this constraint's kind. */
    public boolean accepts(JsonNode value) {
        return switch (kind) {
            case NUMBER -> value.isNumber();
            case COUNT -> value.isIntegralNumber() && value.asLong() >= 0;
            case BOOLEAN -> value.isBoolean();
            case TEXT -> value.isTextual();
        };
    }

    /**
     * Reads what a user typed. A number is written as a number, never as a quoted string — unlike
     * an example value there is no literal fallback, because a string {@code minLength} means
     * nothing to anyone. Any number is taken, though, even for a count: a negative or fractional
     * {@code minLength} can still be written as what it is, and validation reports it — editing an
     * invalid spec is always allowed. Only text that is no number at all is refused.
     *
     * @return the value, or {@code null} for blank text
     * @throws IllegalArgumentException if the text cannot be written as this kind of value
     */
    public JsonNode parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        if (kind == Kind.TEXT) {
            return TextNode.valueOf(text);
        }
        try {
            JsonNode value = JSON.readTree(text.strip());
            boolean writable = kind == Kind.BOOLEAN ? value.isBoolean() : value.isNumber();
            if (writable) {
                return value;
            }
        } catch (JsonProcessingException e) {
            // Falls through to the rejection below.
        }
        throw new IllegalArgumentException("\"" + text + "\" is not a valid " + key);
    }
}
