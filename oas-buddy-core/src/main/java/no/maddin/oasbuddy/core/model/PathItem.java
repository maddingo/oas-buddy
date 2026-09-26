package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.EnumMap;
import java.util.Map;

public final class PathItem {

    private final ObjectNode node;

    public PathItem(ObjectNode node) {
        this.node = node;
    }

    public String getSummary() {
        return JsonNodes.text(node, "summary");
    }

    public void setSummary(String summary) {
        JsonNodes.setText(node, "summary", summary);
    }

    public String getDescription() {
        return JsonNodes.text(node, "description");
    }

    public void setDescription(String description) {
        JsonNodes.setText(node, "description", description);
    }

    /** Parameters declared once for the path, applying to every operation on it. */
    public Parameters getParameters() {
        return new Parameters(node);
    }

    public Map<HttpMethod, Operation> getOperations() {
        Map<HttpMethod, Operation> operations = new EnumMap<>(HttpMethod.class);
        for (HttpMethod method : HttpMethod.values()) {
            var existing = node.get(method.fieldName());
            if (existing instanceof ObjectNode objectNode) {
                operations.put(method, new Operation(objectNode));
            }
        }
        return operations;
    }

    public Operation getOperation(HttpMethod method) {
        var existing = node.get(method.fieldName());
        return existing instanceof ObjectNode objectNode ? new Operation(objectNode) : null;
    }

    public Operation addOperation(HttpMethod method) {
        return new Operation(JsonNodes.objectChild(node, method.fieldName()));
    }

    public void removeOperation(HttpMethod method) {
        node.remove(method.fieldName());
    }
}
