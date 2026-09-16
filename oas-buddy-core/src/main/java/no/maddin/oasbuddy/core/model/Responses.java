package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Responses {

    private final ObjectNode node;

    public Responses(ObjectNode node) {
        this.node = node;
    }

    public List<String> statusCodes() {
        List<String> codes = new ArrayList<>();
        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            codes.add(it.next());
        }
        return codes;
    }

    public ApiResponse getResponse(String statusCode) {
        var existing = node.get(statusCode);
        return existing instanceof ObjectNode objectNode ? new ApiResponse(objectNode) : null;
    }

    public ApiResponse addResponse(String statusCode) {
        return new ApiResponse(JsonNodes.objectChild(node, statusCode));
    }

    public void removeResponse(String statusCode) {
        node.remove(statusCode);
    }
}
