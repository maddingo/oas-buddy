package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.List;

/**
 * The root {@code tags} array: the tags a document defines, in the order they are declared.
 *
 * <p>As with {@link SecurityRequirements}, absent and empty mean the same thing here — a document
 * that defines no tags — so a read never creates the key: {@link #names()} and {@link #get(String)}
 * simply see nothing when {@code tags} is missing, and {@link #remove(String)} takes the key out
 * again once the last tag is gone. That is why this takes the document root rather than an
 * {@code ArrayNode}, the same reason {@code SecurityRequirements} takes a parent and field name.
 */
public final class Tags {

    private static final String FIELD = "tags";
    private static final String NAME_FIELD = "name";

    private final ObjectNode root;

    public Tags(ObjectNode root) {
        this.root = root;
    }

    /** The declared tag names, in document order; empty when nothing is declared. */
    public List<String> names() {
        List<String> names = new ArrayList<>();
        if (root.get(FIELD) instanceof ArrayNode array) {
            for (var element : array) {
                if (element instanceof ObjectNode tag) {
                    String name = JsonNodes.text(tag, NAME_FIELD);
                    if (name != null) {
                        names.add(name);
                    }
                }
            }
        }
        return names;
    }

    /** The named tag, or {@code null} if no tag with that name is declared. */
    public Tag get(String name) {
        if (root.get(FIELD) instanceof ArrayNode array) {
            for (var element : array) {
                if (element instanceof ObjectNode tag && name.equals(JsonNodes.text(tag, NAME_FIELD))) {
                    return new Tag(tag);
                }
            }
        }
        return null;
    }

    /** Adds a tag with this name, or returns the existing one — adding is idempotent by name. */
    public Tag add(String name) {
        Tag existing = get(name);
        if (existing != null) {
            return existing;
        }
        ObjectNode tag = array().addObject();
        tag.put(NAME_FIELD, name);
        return new Tag(tag);
    }

    /** Removes the named tag. Once the last one is gone the {@code tags} key goes with it. */
    public void remove(String name) {
        if (root.get(FIELD) instanceof ArrayNode array) {
            for (int i = 0; i < array.size(); i++) {
                if (array.get(i) instanceof ObjectNode tag && name.equals(JsonNodes.text(tag, NAME_FIELD))) {
                    array.remove(i);
                    break;
                }
            }
            if (array.isEmpty()) {
                root.remove(FIELD);
            }
        }
    }

    private ArrayNode array() {
        return JsonNodes.arrayChild(root, FIELD);
    }
}
