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
 * Finds where a named component — a schema, a response, … — is referenced from, so a caller can
 * tell the user what a removal would break, and rewrites those references on a rename.
 *
 * <p>The search walks the raw document tree rather than the typed facade, because {@code $ref}
 * can appear in places the facade does not expose yet (array {@code items}, schema properties,
 * documents loaded from disk that use features this editor cannot edit). The section is a plain
 * {@code components} field name ({@code "schemas"}, {@code "responses"}), so each kind of component
 * shares this one walk instead of growing a copy of it.
 */
public final class ComponentReferences {

    private static final String REF_FIELD = "$ref";
    private static final String COMPONENTS_PREFIX = "#/components/";
    private static final String LOCATION_SEPARATOR = " → ";

    private ComponentReferences() {
    }

    /** The {@code $ref} text pointing at a component, e.g. {@code #/components/responses/NotFound}. */
    public static String ref(String section, String name) {
        return COMPONENTS_PREFIX + section + "/" + name;
    }

    /**
     * @return the component name {@code ref} points at within {@code section}, or {@code null} if
     *         it points anywhere else
     */
    public static String nameOf(String section, String ref) {
        String prefix = ref(section, "");
        return ref != null && ref.startsWith(prefix) ? ref.substring(prefix.length()) : null;
    }

    /**
     * @return the locations referencing the component, in document order, each rendered as a
     *         readable path such as {@code paths → /pets → get → responses → 200}. Empty if it is
     *         not referenced anywhere.
     */
    public static List<String> find(OasDocument document, String section, String name) {
        List<String> usages = new ArrayList<>();
        String ref = ref(section, name);
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
     * Rewrites every {@code $ref} pointing at component {@code from} to point at {@code to}
     * instead, so a rename never leaves a reference dangling. Shares {@link #find}'s tree walk
     * rather than a second copy of it.
     *
     * @return how many references were rewritten
     */
    public static int rewrite(OasDocument document, String section, String from, String to) {
        String ref = ref(section, from);
        String replacement = ref(section, to);
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
