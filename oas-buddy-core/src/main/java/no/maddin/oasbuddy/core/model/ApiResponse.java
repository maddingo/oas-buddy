package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

public final class ApiResponse {

    private final ObjectNode node;

    public ApiResponse(ObjectNode node) {
        this.node = node;
    }

    public String getDescription() {
        return JsonNodes.text(node, "description");
    }

    public void setDescription(String description) {
        JsonNodes.setText(node, "description", description);
    }

    public Schema getSchema(String mediaType) {
        ObjectNode content = JsonNodes.objectChild(node, "content");
        ObjectNode mediaTypeNode = JsonNodes.objectChild(content, mediaType);
        return new Schema(JsonNodes.objectChild(mediaTypeNode, "schema"));
    }
}
