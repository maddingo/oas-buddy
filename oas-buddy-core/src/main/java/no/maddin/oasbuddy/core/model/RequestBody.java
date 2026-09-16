package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class RequestBody {

    private final ObjectNode node;

    public RequestBody(ObjectNode node) {
        this.node = node;
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

    public List<String> getContentTypes() {
        List<String> types = new ArrayList<>();
        var content = node.get("content");
        if (content instanceof ObjectNode objectNode) {
            Iterator<String> it = objectNode.fieldNames();
            while (it.hasNext()) {
                types.add(it.next());
            }
        }
        return types;
    }

    public Schema getSchema(String mediaType) {
        ObjectNode content = JsonNodes.objectChild(node, "content");
        ObjectNode mediaTypeNode = JsonNodes.objectChild(content, mediaType);
        return new Schema(JsonNodes.objectChild(mediaTypeNode, "schema"));
    }
}
