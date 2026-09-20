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
 * Finds the {@code security} requirements that name a scheme, so a caller can tell the user what a
 * removal would leave dangling.
 *
 * <p>The counterpart to {@link SchemaReferences}, but schemes are referenced by bare name rather
 * than by {@code $ref}: a requirement is an entry in a {@code security} array whose field name is
 * the scheme's. That is why the search keys on the enclosing {@code security} array rather than on
 * the name alone — a schema property or a response header that happens to share the name is not a
 * usage, and reporting it would teach the user to ignore the dialog.
 *
 * <p>Like {@link SchemaReferences} this walks the raw tree, because a {@code security} requirement
 * can sit at the document root, on any operation, and in documents using features the facade does
 * not expose.
 */
public final class SecuritySchemeUsages {

    private static final String SECURITY_FIELD = "security";
    private static final String LOCATION_SEPARATOR = " → ";

    private SecuritySchemeUsages() {
    }

    /**
     * @return the requirements naming {@code schemeName}, in document order, each rendered as a
     *         readable path such as {@code paths → /pets → get → security → [0]}. Empty if nothing
     *         requires the scheme.
     */
    public static List<String> find(OasDocument document, String schemeName) {
        List<String> usages = new ArrayList<>();
        collect(document.getRoot(), schemeName, new ArrayDeque<>(), usages);
        return usages;
    }

    private static void collect(JsonNode node, String schemeName, Deque<String> location, List<String> usages) {
        if (node instanceof ObjectNode objectNode) {
            Iterator<String> fields = objectNode.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                location.addLast(field);
                if (SECURITY_FIELD.equals(field) && objectNode.get(field) instanceof ArrayNode requirements) {
                    collectRequirements(requirements, schemeName, location, usages);
                } else {
                    collect(objectNode.get(field), schemeName, location, usages);
                }
                location.removeLast();
            }
        } else if (node instanceof ArrayNode arrayNode) {
            int index = 0;
            for (JsonNode element : arrayNode) {
                location.addLast("[" + index++ + "]");
                collect(element, schemeName, location, usages);
                location.removeLast();
            }
        }
    }

    private static void collectRequirements(ArrayNode requirements, String schemeName,
                                            Deque<String> location, List<String> usages) {
        int index = 0;
        for (JsonNode requirement : requirements) {
            if (requirement instanceof ObjectNode objectNode && objectNode.has(schemeName)) {
                location.addLast("[" + index + "]");
                usages.add(String.join(LOCATION_SEPARATOR, location));
                location.removeLast();
            }
            index++;
        }
    }
}
