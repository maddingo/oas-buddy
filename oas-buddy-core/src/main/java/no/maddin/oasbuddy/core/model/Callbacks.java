package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An operation's named {@code callbacks}. Reads lazily and takes the key out with the last entry, like
 * {@link ResponseHeaders}, whose in-place inline ↔ {@code $ref} switching it shares: replacing an
 * existing key keeps its position, so the callbacks never reorder.
 */
public final class Callbacks {

    private static final String FIELD = "callbacks";

    private final ObjectNode parent;

    Callbacks(ObjectNode parent) {
        this.parent = parent;
    }

    /** Every key, including one whose value is not an object, so the file is never hidden from the user. */
    public List<String> names() {
        List<String> names = new ArrayList<>();
        if (parent.get(FIELD) instanceof ObjectNode callbacks) {
            Iterator<String> it = callbacks.fieldNames();
            while (it.hasNext()) {
                names.add(it.next());
            }
        }
        return names;
    }

    /** The named callback, or {@code null} if it is absent or not an object. */
    public Callback get(String name) {
        return parent.get(FIELD) instanceof ObjectNode callbacks && callbacks.get(name) instanceof ObjectNode callback
                ? new Callback(callback)
                : null;
    }

    /** Adds an empty inline callback, or returns the existing one of that name. */
    public Callback add(String name) {
        return new Callback(JsonNodes.objectChild(JsonNodes.objectChild(parent, FIELD), name));
    }

    public void remove(String name) {
        if (parent.get(FIELD) instanceof ObjectNode callbacks) {
            callbacks.remove(name);
            if (callbacks.isEmpty()) {
                parent.remove(FIELD);
            }
        }
    }

    /** In place, so the saved file is not reordered; {@code false} means refused. See {@link JsonNodes#renameField}. */
    public boolean rename(String from, String to) {
        return parent.get(FIELD) instanceof ObjectNode callbacks && JsonNodes.renameField(callbacks, from, to);
    }

    /**
     * Makes the named entry a reference to a component callback, replacing whatever was there — an
     * inline callback included, so a caller that cares asks first (see {@link Callback#isEmpty}).
     */
    public Callback referTo(String name, String componentName) {
        ObjectNode ref = JsonNodeFactory.instance.objectNode();
        ref.put("$ref", ComponentReferences.ref(ComponentCallbacks.SECTION, componentName));
        JsonNodes.objectChild(parent, FIELD).set(name, ref);
        return new Callback(ref);
    }

    /** Makes the named entry inline, in place, from a copy of {@code template} (what the reference pointed at). */
    public Callback defineInline(String name, Callback template) {
        ObjectNode inline = template != null ? template.node().deepCopy() : JsonNodeFactory.instance.objectNode();
        JsonNodes.objectChild(parent, FIELD).set(name, inline);
        return new Callback(inline);
    }
}
