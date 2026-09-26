package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

/**
 * A parameter: either defined inline, or a {@code $ref} to one in {@code components.parameters}.
 * Switching between the two replaces the whole object, so that is {@link Parameters}' business.
 */
public final class Parameter {

    private static final String REF = "$ref";

    private final ObjectNode node;

    public Parameter(ObjectNode node) {
        this.node = node;
    }

    ObjectNode node() {
        return node;
    }

    public String getRef() {
        return JsonNodes.text(node, REF);
    }

    /** The component parameter this refers to, or {@code null} if it is inline (or refers elsewhere). */
    public String getReferencedParameterName() {
        return ComponentReferences.nameOf(ComponentParameters.SECTION, getRef());
    }

    public boolean isReference() {
        return node.has(REF);
    }

    public String getName() {
        return JsonNodes.text(node, "name");
    }

    public void setName(String name) {
        JsonNodes.setText(node, "name", name);
    }

    public String getIn() {
        return JsonNodes.text(node, "in");
    }

    public void setIn(String in) {
        JsonNodes.setText(node, "in", in);
    }

    public String getDescription() {
        return JsonNodes.text(node, "description");
    }

    public void setDescription(String description) {
        JsonNodes.setText(node, "description", description);
    }

    public Boolean isRequired() {
        return JsonNodes.bool(node, "required");
    }

    public void setRequired(Boolean required) {
        JsonNodes.setBool(node, "required", required);
    }

    /** The schema, creating it if absent. */
    public Schema getSchema() {
        return new Schema(JsonNodes.objectChild(node, "schema"));
    }

    /** The schema if there is one, otherwise {@code null}. Reads only. */
    public Schema findSchema() {
        return node.get("schema") instanceof ObjectNode schemaNode ? new Schema(schemaNode) : null;
    }
}
