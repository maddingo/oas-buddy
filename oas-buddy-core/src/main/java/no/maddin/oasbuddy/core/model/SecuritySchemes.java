package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;
import no.maddin.oasbuddy.core.document.LazyObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** The named map at {@code components.securitySchemes}. */
public final class SecuritySchemes {

    private final LazyObjectNode node;

    public SecuritySchemes(LazyObjectNode node) {
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

    public SecurityScheme getScheme(String name) {
        ObjectNode existing = node.peek();
        return existing != null && existing.get(name) instanceof ObjectNode schemeNode
                ? new SecurityScheme(schemeNode)
                : null;
    }

    public SecurityScheme addScheme(String name) {
        return new SecurityScheme(JsonNodes.objectChild(node.create(), name));
    }

    public void removeScheme(String name) {
        ObjectNode existing = node.peek();
        if (existing != null) {
            existing.remove(name);
        }
    }
}
