package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Schemas {

    private final ObjectNode node;

    public Schemas(ObjectNode node) {
        this.node = node;
    }

    public List<String> names() {
        List<String> names = new ArrayList<>();
        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            names.add(it.next());
        }
        return names;
    }

    public Schema getSchema(String name) {
        var existing = node.get(name);
        return existing instanceof ObjectNode objectNode ? new Schema(objectNode) : null;
    }

    public Schema addSchema(String name) {
        return new Schema(JsonNodes.objectChild(node, name));
    }

    public void removeSchema(String name) {
        node.remove(name);
    }
}
