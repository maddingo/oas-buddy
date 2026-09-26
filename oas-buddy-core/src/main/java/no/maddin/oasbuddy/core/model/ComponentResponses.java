package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;
import no.maddin.oasbuddy.core.document.LazyObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The named map at {@code components.responses}: responses defined once and referenced from any
 * number of operations. Named for where it lives, since {@link Responses} is already an
 * operation's own status-code map.
 */
public final class ComponentResponses {

    /** The {@code components} field these live in, as {@link ComponentReferences} names it. */
    public static final String SECTION = "responses";

    private final LazyObjectNode node;

    public ComponentResponses(LazyObjectNode node) {
        this.node = node;
    }

    public List<String> names() {
        ObjectNode existing = node.peek();
        if (existing == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        Iterator<String> it = existing.fieldNames();
        while (it.hasNext()) {
            names.add(it.next());
        }
        return names;
    }

    public ApiResponse getResponse(String name) {
        ObjectNode existing = node.peek();
        return existing != null && existing.get(name) instanceof ObjectNode responseNode
                ? new ApiResponse(responseNode)
                : null;
    }

    public ApiResponse addResponse(String name) {
        return new ApiResponse(JsonNodes.objectChild(node.create(), name));
    }

    public void removeResponse(String name) {
        ObjectNode existing = node.peek();
        if (existing != null) {
            existing.remove(name);
        }
    }
}
