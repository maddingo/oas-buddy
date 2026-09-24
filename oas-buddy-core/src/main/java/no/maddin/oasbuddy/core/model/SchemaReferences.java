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
        String ref = REF_PREFIX + schemaName;
        walk(document.getRoot(), new ArrayDeque<>(), (node, location) -> {
            boolean matches = ref.equals(text(node.get(REF_FIELD)));
            if (matches) {
                usages.add(String.join(LOCATION_SEPARATOR, location));
            }
            return matches;
        });
        return usages;
    }

    /**
     * Rewrites every {@code $ref} pointing at {@code from} to point at {@code to} instead, so a
     * schema rename never leaves a reference dangling. Shares {@link #find}'s tree walk rather than
     * a second copy of it.
     *
     * @return how many references were rewritten
     */
    public static int rewrite(OasDocument document, String from, String to) {
        String ref = REF_PREFIX + from;
        String replacement = REF_PREFIX + to;
        int[] count = {0};
        walk(document.getRoot(), new ArrayDeque<>(), (node, location) -> {
            boolean matches = ref.equals(text(node.get(REF_FIELD)));
            if (matches) {
                node.put(REF_FIELD, replacement);
                count[0]++;
            }
            return matches;
        });
        return count[0];
    }

    /**
     * Visits every object in the tree, depth first, tracking the field/index path down to it.
     *
     * @return whether {@code node} itself was a match — when it is, its children are not walked,
     *         since OAS ignores whatever sits beside a {@code $ref} and there is nothing there to find
     */
    @FunctionalInterface
    private interface RefVisitor {
        boolean visit(ObjectNode node, Deque<String> location);
    }

    private static void walk(JsonNode node, Deque<String> location, RefVisitor visitor) {
        if (node instanceof ObjectNode objectNode) {
            if (visitor.visit(objectNode, location)) {
                return;
            }
            Iterator<String> fields = objectNode.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                location.addLast(field);
                walk(objectNode.get(field), location, visitor);
                location.removeLast();
            }
        } else if (node instanceof ArrayNode arrayNode) {
            int index = 0;
            for (JsonNode element : arrayNode) {
                location.addLast("[" + index++ + "]");
                walk(element, location, visitor);
                location.removeLast();
            }
        }
    }

    private static String text(JsonNode node) {
        return node == null || !node.isTextual() ? null : node.asText();
    }
}
