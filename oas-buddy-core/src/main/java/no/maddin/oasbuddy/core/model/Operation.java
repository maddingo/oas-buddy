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

    public void setTags(List<String> tags) {
        ArrayNode array = JsonNodes.arrayChild(node, "tags");
        array.removeAll();
        for (String tag : tags) {
            array.add(tag);
        }
    }

    public List<Parameter> getParameters() {
        List<Parameter> parameters = new ArrayList<>();
        var existing = node.get("parameters");
        if (existing instanceof ArrayNode arrayNode) {
            for (var element : arrayNode) {
                if (element instanceof ObjectNode objectNode) {
                    parameters.add(new Parameter(objectNode));
                }
            }
        }
        return parameters;
    }

    public Parameter addParameter(String name, String in) {
        ArrayNode array = JsonNodes.arrayChild(node, "parameters");
        ObjectNode paramNode = array.addObject();
        Parameter parameter = new Parameter(paramNode);
        parameter.setName(name);
        parameter.setIn(in);
        return parameter;
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
