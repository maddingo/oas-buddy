package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

/**
 * A response: either defined inline, or a {@code $ref} to one in {@code components.responses}.
 * Which of the two it is, and switching between them, is {@link Responses}' business, since
 * switching replaces the whole object.
 */
public final class ApiResponse {

    private static final String REF = "$ref";
    private static final String DESCRIPTION = "description";

    private final ObjectNode node;

    public ApiResponse(ObjectNode node) {
        this.node = node;
    }

    ObjectNode node() {
        return node;
    }

    public String getDescription() {
        return JsonNodes.text(node, DESCRIPTION);
    }

    public void setDescription(String description) {
        JsonNodes.setText(node, DESCRIPTION, description);
    }

    public String getRef() {
        return JsonNodes.text(node, REF);
    }

    /** The component response this refers to, or {@code null} if it is inline (or refers elsewhere). */
    public String getReferencedResponseName() {
        return ComponentReferences.nameOf(ComponentResponses.SECTION, getRef());
    }

    public boolean isReference() {
        return node.has(REF);
    }

    /**
     * Whether there is nothing here to lose: no field at all, or only a blank description. What
     * decides if replacing an inline response with a reference needs asking about first.
     */
    public boolean isEmpty() {
        if (node.isEmpty()) {
            return true;
        }
        String description = getDescription();
        return node.size() == 1 && node.has(DESCRIPTION) && (description == null || description.isBlank());
    }

    /** The schema for {@code mediaType}, creating it and any missing parent. */
    public Schema getSchema(String mediaType) {
        ObjectNode content = JsonNodes.objectChild(node, "content");
        ObjectNode mediaTypeNode = JsonNodes.objectChild(content, mediaType);
        return new Schema(JsonNodes.objectChild(mediaTypeNode, "schema"));
    }

    /** The schema for {@code mediaType} if it is there, otherwise {@code null}. Reads only. */
    public Schema findSchema(String mediaType) {
        return node.get("content") instanceof ObjectNode content
                && content.get(mediaType) instanceof ObjectNode mediaTypeNode
                && mediaTypeNode.get("schema") instanceof ObjectNode schemaNode
                ? new Schema(schemaNode)
                : null;
    }
}
