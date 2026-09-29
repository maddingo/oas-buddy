package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A response's named {@code headers}. Reads lazily and takes the key out with the last entry, like
 * {@link MediaTypeExamples}, whose in-place inline ↔ {@code $ref} switching it shares: replacing an
 * existing key keeps its position, so the headers never reorder.
 */
public final class ResponseHeaders {

    private static final String FIELD = "headers";

    private final ObjectNode parent;

    ResponseHeaders(ObjectNode parent) {
        this.parent = parent;
    }

    /** Every key, including one whose value is not an object, so the file is never hidden from the user. */
    public List<String> names() {
        List<String> names = new ArrayList<>();
        if (parent.get(FIELD) instanceof ObjectNode headers) {
            Iterator<String> it = headers.fieldNames();
            while (it.hasNext()) {
                names.add(it.next());
            }
        }
        return names;
    }

    /** The named header, or {@code null} if it is absent or not an object. */
    public Header get(String name) {
        return parent.get(FIELD) instanceof ObjectNode headers && headers.get(name) instanceof ObjectNode header
                ? new Header(header)
                : null;
    }

    /** Adds an empty inline header, or returns the existing one of that name. */
    public Header add(String name) {
        return new Header(JsonNodes.objectChild(JsonNodes.objectChild(parent, FIELD), name));
    }

    public void remove(String name) {
        if (parent.get(FIELD) instanceof ObjectNode headers) {
            headers.remove(name);
            if (headers.isEmpty()) {
                parent.remove(FIELD);
            }
        }
    }

    /** In place, so the saved file is not reordered; {@code false} means refused. See {@link JsonNodes#renameField}. */
    public boolean rename(String from, String to) {
        return parent.get(FIELD) instanceof ObjectNode headers && JsonNodes.renameField(headers, from, to);
    }

    /**
     * Makes the named entry a reference to a component header, replacing whatever was there — an
     * inline header included, so a caller that cares asks first (see {@link Header#isEmpty}).
     */
    public Header referTo(String name, String componentName) {
        ObjectNode ref = JsonNodeFactory.instance.objectNode();
        ref.put("$ref", ComponentReferences.ref(ComponentHeaders.SECTION, componentName));
        JsonNodes.objectChild(parent, FIELD).set(name, ref);
        return new Header(ref);
    }

    /** Makes the named entry inline, in place, from a copy of {@code template} (what the reference pointed at). */
    public Header defineInline(String name, Header template) {
        ObjectNode inline = template != null ? template.node().deepCopy() : JsonNodeFactory.instance.objectNode();
        JsonNodes.objectChild(parent, FIELD).set(name, inline);
        return new Header(inline);
    }
}
