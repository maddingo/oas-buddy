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

    public ComponentResponses getResponses() {
        return new ComponentResponses(node.child(ComponentResponses.SECTION));
    }

    public ComponentParameters getParameters() {
        return new ComponentParameters(node.child(ComponentParameters.SECTION));
    }

    public SecuritySchemes getSecuritySchemes() {
        return new SecuritySchemes(node.child("securitySchemes"));
    }
}
