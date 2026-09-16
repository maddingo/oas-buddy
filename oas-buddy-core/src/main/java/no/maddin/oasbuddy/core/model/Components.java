package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

public final class Components {

    private final ObjectNode node;

    public Components(ObjectNode node) {
        this.node = node;
    }

    public Schemas getSchemas() {
        return new Schemas(JsonNodes.objectChild(node, "schemas"));
    }
}
