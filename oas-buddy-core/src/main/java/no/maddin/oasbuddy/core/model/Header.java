package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A header: either defined inline, or a {@code $ref} to one in {@code components.headers}.
 *
 * <p>OAS defines a header as a parameter without {@code name} and {@code in}, so this wraps a
 * {@link Parameter} over the same node and exposes the fields that apply, rather than repeating
 * them. Whatever {@code Parameter} gains that a header shares (its serialization detail) becomes
 * reachable here by adding one delegating method.
 */
public final class Header {

    private final Parameter parameter;
    private final ObjectNode node;

    public Header(ObjectNode node) {
        this.node = node;
        this.parameter = new Parameter(node);
    }

    ObjectNode node() {
        return node;
    }

    public String getRef() {
        return parameter.getRef();
    }

    public boolean isReference() {
        return parameter.isReference();
    }

    /** The component header this refers to, or {@code null} if it is inline (or refers elsewhere). */
    public String getReferencedHeaderName() {
        return ComponentReferences.nameOf(ComponentHeaders.SECTION, getRef());
    }

    public String getDescription() {
        return parameter.getDescription();
    }

    public void setDescription(String description) {
        parameter.setDescription(description);
    }

    public Boolean isRequired() {
        return parameter.isRequired();
    }

    public void setRequired(Boolean required) {
        parameter.setRequired(required);
    }

    /** The schema, creating it if absent. */
    public Schema getSchema() {
        return parameter.getSchema();
    }

    /** The schema if there is one, otherwise {@code null}. Reads only. */
    public Schema findSchema() {
        return parameter.findSchema();
    }

    /** Whether there is nothing here to lose; what decides if replacing an inline header needs asking first. */
    public boolean isEmpty() {
        return node.isEmpty();
    }
}
