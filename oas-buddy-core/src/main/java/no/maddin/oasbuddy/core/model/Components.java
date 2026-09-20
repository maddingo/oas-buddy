package no.maddin.oasbuddy.core.model;

import no.maddin.oasbuddy.core.document.LazyObjectNode;

public final class Components {

    private final LazyObjectNode node;

    public Components(LazyObjectNode node) {
        this.node = node;
    }

    public Schemas getSchemas() {
        return new Schemas(node.child("schemas"));
    }

    public SecuritySchemes getSecuritySchemes() {
        return new SecuritySchemes(node.child("securitySchemes"));
    }
}
