package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

/**
 * The {@code externalDocs} object that can hang off the document root, a tag, an operation and a
 * schema: a {@code description} and a {@code url}.
 *
 * <p>It takes the <em>parent</em> node rather than the object itself, the same reason
 * {@link SecurityRequirements} does: a read must never add an empty {@code externalDocs}, and
 * clearing both fields must take the key out again rather than leave {@code {}} behind. An
 * {@code x-} extension inside the object counts as content, so it keeps the object alive.
 *
 * <p>An {@code externalDocs} that exists but is not an object (a hand-edited {@code ~} or string)
 * reads as empty and is {@link #isEditable() not editable}: writing would replace it, and the
 * editor never rewrites what it cannot show.
 */
public final class ExternalDocs {

    private static final String FIELD = "externalDocs";

    private final ObjectNode parent;

    public ExternalDocs(ObjectNode parent) {
        this.parent = parent;
    }

    /** {@code false} when the key holds something other than an object; see the class comment. */
    public boolean isEditable() {
        JsonNode existing = parent.get(FIELD);
        return existing == null || existing instanceof ObjectNode;
    }

    public String getDescription() {
        return read("description");
    }

    public void setDescription(String description) {
        write("description", description);
    }

    public String getUrl() {
        return read("url");
    }

    public void setUrl(String url) {
        write("url", url);
    }

    private String read(String field) {
        return parent.get(FIELD) instanceof ObjectNode node ? JsonNodes.text(node, field) : null;
    }

    private void write(String field, String value) {
        if (!isEditable()) {
            return;
        }
        String text = value == null || value.isBlank() ? null : value;
        if (parent.get(FIELD) instanceof ObjectNode node) {
            JsonNodes.setText(node, field, text);
            if (node.isEmpty()) {
                parent.remove(FIELD);
            }
        } else if (text != null) {
            JsonNodes.setText(JsonNodes.objectChild(parent, FIELD), field, text);
        }
    }
}
