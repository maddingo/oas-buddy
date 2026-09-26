package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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

    /**
     * Makes the response for {@code statusCode} a reference to a component response, replacing
     * whatever was there — an inline definition included, so a caller that cares asks first (see
     * {@link ApiResponse#isEmpty}). The status code keeps its position among the others.
     */
    public ApiResponse referTo(String statusCode, String responseName) {
        ObjectNode ref = JsonNodeFactory.instance.objectNode();
        ref.put("$ref", ComponentReferences.ref(ComponentResponses.SECTION, responseName));
        node.set(statusCode, ref);
        return new ApiResponse(ref);
    }

    /**
     * Makes the response for {@code statusCode} an inline definition, replacing a reference. It
     * starts as a copy of {@code template} — the response the reference pointed at, so switching
     * to inline loses nothing — or, without one, as a bare response with an empty description,
     * the one field OAS requires. The status code keeps its position among the others.
     */
    public ApiResponse defineInline(String statusCode, ApiResponse template) {
        ObjectNode inline;
        if (template != null) {
            inline = template.node().deepCopy();
        } else {
            inline = JsonNodeFactory.instance.objectNode();
            inline.put("description", "");
        }
        node.set(statusCode, inline);
        return new ApiResponse(inline);
    }
}
