package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.OasDocument;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Finds where a component schema is referenced from, so a caller can tell the user what a
 * removal would break.
 *
 * <p>The search walks the raw document tree rather than the typed facade, because {@code $ref}
 * can appear in places the facade does not expose yet (array {@code items}, schema properties,
 * documents loaded from disk that use features this editor cannot edit).
 */
public final class SchemaReferences {

    private static final String REF_FIELD = "$ref";
    private static final String REF_PREFIX = "#/components/schemas/";
    private static final String LOCATION_SEPARATOR = " → ";

    private SchemaReferences() {
    }

    /**
     * @return the locations referencing {@code schemaName}, in document order, each rendered as a
     *         readable path such as {@code paths → /pets → get → responses → 200}. Empty if the
     *         schema is not referenced anywhere.
     */
    public static List<String> find(OasDocument document, String schemaName) {
        List<String> usages = new ArrayList<>();
        collect(document.getRoot(), REF_PREFIX + schemaName, new ArrayDeque<>(), usages);
        return usages;
    }

    private static void collect(JsonNode node, String ref, Deque<String> location, List<String> usages) {
        if (node instanceof ObjectNode objectNode) {
            if (ref.equals(text(objectNode.get(REF_FIELD)))) {
                usages.add(String.join(LOCATION_SEPARATOR, location));
                return;
            }
            Iterator<String> fields = objectNode.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                location.addLast(field);
                collect(objectNode.get(field), ref, location, usages);
                location.removeLast();
            }
        } else if (node instanceof ArrayNode arrayNode) {
            int index = 0;
            for (JsonNode element : arrayNode) {
                location.addLast("[" + index++ + "]");
                collect(element, ref, location, usages);
                location.removeLast();
            }
        }
    }

    private static String text(JsonNode node) {
        return node == null || !node.isTextual() ? null : node.asText();
    }
}
