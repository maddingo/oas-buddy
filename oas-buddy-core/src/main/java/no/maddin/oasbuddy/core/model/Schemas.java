package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;
import no.maddin.oasbuddy.core.document.LazyObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Schemas {

    private final LazyObjectNode node;

    public Schemas(LazyObjectNode node) {
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

    public Schema getSchema(String name) {
        ObjectNode existing = node.peek();
        return existing != null && existing.get(name) instanceof ObjectNode schemaNode
                ? new Schema(schemaNode)
                : null;
    }

    public Schema addSchema(String name) {
        return new Schema(JsonNodes.objectChild(node.create(), name));
    }

    public void removeSchema(String name) {
        ObjectNode existing = node.peek();
        if (existing != null) {
            existing.remove(name);
        }
    }

    /**
     * Renames a schema, keeping its position in the document. Does not touch any {@code $ref}
     * that points at it — see {@link SchemaReferences#rewrite}.
     *
     * @return {@code false}, changing nothing, if {@code from} does not exist or {@code to} is
     *         already taken (a collision is refused, never silently overwritten)
     */
    public boolean renameSchema(String from, String to) {
        ObjectNode existing = node.peek();
        return existing != null && JsonNodes.renameField(existing, from, to);
    }
}
