package no.maddin.oasbuddy.core.document;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link JsonNodes#renameField}, the one place a field is renamed without moving it. */
class JsonNodesTest {

    @Test
    void renamesAFieldInPlaceKeepingTheOrderOfEverythingElse() {
        ObjectNode node = objectOf("a", "b", "c");

        boolean renamed = JsonNodes.renameField(node, "b", "z");

        assertFieldOrder(node, "a", "z", "c");
        assertTrue(renamed);
    }

    @Test
    void keepsTheSameValueInstanceRatherThanACopy() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        ObjectNode value = node.putObject("original");
        value.put("nested", true);

        JsonNodes.renameField(node, "original", "renamed");

        assertSame(value, node.get("renamed"));
    }

    @Test
    void refusesACollisionAndChangesNothing() {
        ObjectNode node = objectOf("a", "b");
        node.put("a", "A");
        node.put("b", "B");

        boolean renamed = JsonNodes.renameField(node, "a", "b");

        assertFalse(renamed);
        assertEquals("A", node.get("a").asText());
        assertEquals("B", node.get("b").asText());
        assertEquals(List.of("a", "b"), fieldNames(node));
    }

    @Test
    void refusesWhenFromIsAbsent() {
        ObjectNode node = objectOf("a");

        assertFalse(JsonNodes.renameField(node, "nope", "z"));
        assertEquals(List.of("a"), fieldNames(node));
    }

    @Test
    void refusesABlankOrEqualRename() {
        ObjectNode node = objectOf("a");

        assertFalse(JsonNodes.renameField(node, "a", "a"));
        assertFalse(JsonNodes.renameField(node, "a", ""));
        assertFalse(JsonNodes.renameField(node, "a", " "));
        assertFalse(JsonNodes.renameField(node, "", "z"));
        assertEquals(List.of("a"), fieldNames(node));
    }

    private static void assertFieldOrder(ObjectNode node, String... expectedOrder) {
        assertEquals(List.of(expectedOrder), fieldNames(node));
    }

    private static List<String> fieldNames(ObjectNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static ObjectNode objectOf(String... fields) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        for (String field : fields) {
            node.put(field, field.toUpperCase());
        }
        return node;
    }
}
