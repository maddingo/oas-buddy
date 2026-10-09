package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;
import no.maddin.oasbuddy.core.document.LazyObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** The named map at {@code components.callbacks}: callbacks defined once and referenced from operations. */
public final class ComponentCallbacks {

    /** The {@code components} field these live in, as {@link ComponentReferences} names it. */
    public static final String SECTION = "callbacks";

    private final LazyObjectNode node;

    public ComponentCallbacks(LazyObjectNode node) {
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

    public Callback getCallback(String name) {
        ObjectNode existing = node.peek();
        return existing != null && existing.get(name) instanceof ObjectNode callbackNode
                ? new Callback(callbackNode)
                : null;
    }

    public Callback addCallback(String name) {
        return new Callback(JsonNodes.objectChild(node.create(), name));
    }

    public void removeCallback(String name) {
        ObjectNode existing = node.peek();
        if (existing != null) {
            existing.remove(name);
        }
    }
}
