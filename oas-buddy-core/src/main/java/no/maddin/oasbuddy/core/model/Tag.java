package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

/**
 * One entry in the root {@code tags} array.
 *
 * <p>Only {@code name} and {@code description} are editable for now; {@code externalDocs} and any
 * {@code x-} extension are left untouched, the same bargain {@code SecurityScheme.setType} makes for
 * fields it cannot edit. There is deliberately no {@code setName}: renaming a tag would need to walk
 * every operation that names it, which is out of scope for now — {@link Tags#remove} and
 * {@link TagUsages} are enough to let a user delete and recreate one instead.
 */
public final class Tag {

    private final ObjectNode node;

    public Tag(ObjectNode node) {
        this.node = node;
    }

    public String getName() {
        return JsonNodes.text(node, "name");
    }

    public String getDescription() {
        return JsonNodes.text(node, "description");
    }

    public void setDescription(String description) {
        JsonNodes.setText(node, "description", description);
    }
}
