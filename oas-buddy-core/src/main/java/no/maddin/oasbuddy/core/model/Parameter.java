package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

public final class Parameter {

    private final ObjectNode node;

    public Parameter(ObjectNode node) {
        this.node = node;
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

    public Schema getSchema() {
        return new Schema(JsonNodes.objectChild(node, "schema"));
    }
}
