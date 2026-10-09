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

    public ComponentExamples getExamples() {
        return new ComponentExamples(node.child(ComponentExamples.SECTION));
    }

    public ComponentHeaders getHeaders() {
        return new ComponentHeaders(node.child(ComponentHeaders.SECTION));
    }

    public ComponentLinks getLinks() {
        return new ComponentLinks(node.child(ComponentLinks.SECTION));
    }

    public ComponentCallbacks getCallbacks() {
        return new ComponentCallbacks(node.child(ComponentCallbacks.SECTION));
    }

    public SecuritySchemes getSecuritySchemes() {
        return new SecuritySchemes(node.child("securitySchemes"));
    }
}
