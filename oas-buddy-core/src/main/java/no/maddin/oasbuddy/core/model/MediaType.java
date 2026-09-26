package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

/**
 * One entry of a {@link Content} map: its {@code schema}, a single {@code example}, and named
 * {@code examples}. OAS makes {@code example} and {@code examples} mutually exclusive; both are
 * editable here and a document using both is left for validation to report.
 */
public final class MediaType {

    private static final String EXAMPLE = "example";

    private final ObjectNode node;

    public MediaType(ObjectNode node) {
        this.node = node;
    }

    /** The schema, creating it if absent. */
    public Schema getSchema() {
        return new Schema(JsonNodes.objectChild(node, "schema"));
    }

    /** The schema if there is one, otherwise {@code null}. Reads only. */
    public Schema findSchema() {
        return node.get("schema") instanceof ObjectNode schemaNode ? new Schema(schemaNode) : null;
    }

    public void removeSchema() {
        node.remove("schema");
    }

    public JsonNode getExample() {
        return node.get(EXAMPLE);
    }

    /** {@code null} removes the key. */
    public void setExample(JsonNode example) {
        if (example == null) {
            node.remove(EXAMPLE);
        } else {
            node.set(EXAMPLE, example);
        }
    }

    /** The named {@code examples}, each inline or a reference to a component example. */
    public MediaTypeExamples getExamples() {
        return new MediaTypeExamples(node);
    }
}
