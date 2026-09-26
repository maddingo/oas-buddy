package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

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

    /** The media types this body accepts. */
    public Content getContent() {
        return new Content(node);
    }

    public List<String> getContentTypes() {
        return getContent().mediaTypes();
    }

    /** The schema for {@code mediaType}, creating it and any missing parent. */
    public Schema getSchema(String mediaType) {
        return getContent().add(mediaType).getSchema();
    }
}
