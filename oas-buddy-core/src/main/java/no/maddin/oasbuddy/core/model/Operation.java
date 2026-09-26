package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.List;

public final class Operation {

    private final ObjectNode node;

    public Operation(ObjectNode node) {
        this.node = node;
    }

    /**
     * This operation's security override. Absent means it inherits the document default; see
     * {@link SecurityRequirements}.
     */
    public SecurityRequirements getSecurity() {
        return new SecurityRequirements(node);
    }

    public String getOperationId() {
        return JsonNodes.text(node, "operationId");
    }

    public void setOperationId(String operationId) {
        JsonNodes.setText(node, "operationId", operationId);
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

    public List<String> getTags() {
        List<String> tags = new ArrayList<>();
        var existing = node.get("tags");
        if (existing instanceof ArrayNode arrayNode) {
            for (var element : arrayNode) {
                tags.add(element.asText());
            }
        }
        return tags;
    }

    /** An empty list removes the {@code tags} key rather than leaving {@code []}. */
    public void setTags(List<String> tags) {
        if (tags.isEmpty()) {
            node.remove("tags");
            return;
        }
        ArrayNode array = JsonNodes.arrayChild(node, "tags");
        array.removeAll();
        for (String tag : tags) {
            array.add(tag);
        }
    }

    /** This operation's own parameters; path-level ones apply too, see {@link PathItem#getParameters}. */
    public Parameters getParameters() {
        return new Parameters(node);
    }

    public RequestBody getRequestBody() {
        var existing = node.get("requestBody");
        return existing instanceof ObjectNode objectNode ? new RequestBody(objectNode) : null;
    }

    public RequestBody addRequestBody() {
        return new RequestBody(JsonNodes.objectChild(node, "requestBody"));
    }

    public Responses getResponses() {
        return new Responses(JsonNodes.objectChild(node, "responses"));
    }
}
