package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

/**
 * A Link object — {@code operationId} or {@code operationRef}, {@code parameters}, {@code requestBody},
 * {@code description}, {@code server} — or a {@code $ref} to one in {@code components.links}.
 *
 * <p>The target is held as two separate fields, as OAS has it: setting one does not clear the other,
 * since a file carrying both is a spec violation for validation to report, not for this facade to
 * quietly repair.
 */
public final class Link {

    private static final String REF = "$ref";
    private static final String REQUEST_BODY = "requestBody";
    private static final String SERVER = "server";

    private final ObjectNode node;

    public Link(ObjectNode node) {
        this.node = node;
    }

    ObjectNode node() {
        return node;
    }

    public String getRef() {
        return JsonNodes.text(node, REF);
    }

    /** The component link this refers to, or {@code null} if it is inline (or refers elsewhere). */
    public String getReferencedLinkName() {
        return ComponentReferences.nameOf(ComponentLinks.SECTION, getRef());
    }

    public boolean isReference() {
        return node.has(REF);
    }

    /** Whether there is nothing to lose: no field at all. */
    public boolean isEmpty() {
        return node.isEmpty();
    }

    public String getOperationId() {
        return JsonNodes.text(node, "operationId");
    }

    public void setOperationId(String operationId) {
        JsonNodes.setText(node, "operationId", operationId);
    }

    public String getOperationRef() {
        return JsonNodes.text(node, "operationRef");
    }

    public void setOperationRef(String operationRef) {
        JsonNodes.setText(node, "operationRef", operationRef);
    }

    public String getDescription() {
        return JsonNodes.text(node, "description");
    }

    public void setDescription(String description) {
        JsonNodes.setText(node, "description", description);
    }

    /** A runtime expression or a literal of any JSON type; {@code null} if absent. */
    public JsonNode getRequestBody() {
        return node.get(REQUEST_BODY);
    }

    /** {@code null} removes the key. */
    public void setRequestBody(JsonNode requestBody) {
        if (requestBody == null) {
            node.remove(REQUEST_BODY);
        } else {
            node.set(REQUEST_BODY, requestBody);
        }
    }

    /** The parameters passed to the target operation. Reading never adds the key. */
    public LinkParameters getParameters() {
        return new LinkParameters(node);
    }

    /** The server override, or {@code null} if there is none (or it is not an object). Reads only. */
    public Server getServer() {
        return node.get(SERVER) instanceof ObjectNode server ? new Server(server) : null;
    }

    public Server createServer() {
        return new Server(JsonNodes.objectChild(node, SERVER));
    }

    public void removeServer() {
        node.remove(SERVER);
    }
}
