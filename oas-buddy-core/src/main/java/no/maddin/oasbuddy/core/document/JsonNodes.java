package no.maddin.oasbuddy.core.document;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class JsonNodes {

    private JsonNodes() {
    }

    public static ObjectNode objectChild(ObjectNode parent, String field) {
        var existing = parent.get(field);
        if (existing instanceof ObjectNode objectNode) {
            return objectNode;
        }
        ObjectNode created = JsonNodeFactory.instance.objectNode();
        parent.set(field, created);
        return created;
    }

    public static ArrayNode arrayChild(ObjectNode parent, String field) {
        var existing = parent.get(field);
        if (existing instanceof ArrayNode arrayNode) {
            return arrayNode;
        }
        ArrayNode created = JsonNodeFactory.instance.arrayNode();
        parent.set(field, created);
        return created;
    }

    public static String text(ObjectNode node, String field) {
        var value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    public static void setText(ObjectNode node, String field, String value) {
        if (value == null) {
            node.remove(field);
        } else {
            node.put(field, value);
        }
    }

    public static Boolean bool(ObjectNode node, String field) {
        var value = node.get(field);
        return value == null || value.isNull() ? null : value.asBoolean();
    }

    public static void setBool(ObjectNode node, String field, Boolean value) {
        if (value == null) {
            node.remove(field);
        } else {
            node.put(field, value);
        }
    }
}
