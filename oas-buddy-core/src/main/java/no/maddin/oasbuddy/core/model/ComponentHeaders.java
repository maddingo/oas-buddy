package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;
import no.maddin.oasbuddy.core.document.LazyObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** The named map at {@code components.headers}: headers defined once and referenced from responses. */
public final class ComponentHeaders {

    /** The {@code components} field these live in, as {@link ComponentReferences} names it. */
    public static final String SECTION = "headers";

    private final LazyObjectNode node;

    public ComponentHeaders(LazyObjectNode node) {
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

    /** The named header, or {@code null} if it is absent or not an object. */
    public Header getHeader(String name) {
        ObjectNode existing = node.peek();
        return existing != null && existing.get(name) instanceof ObjectNode headerNode
                ? new Header(headerNode)
                : null;
    }

    public Header addHeader(String name) {
        return new Header(JsonNodes.objectChild(node.create(), name));
    }

    public void removeHeader(String name) {
        ObjectNode existing = node.peek();
        if (existing != null) {
            existing.remove(name);
        }
    }
}
