package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A response's named {@code links}. Reads lazily and takes the key out with the last entry, like
 * {@link MediaTypeExamples}, whose in-place inline ↔ {@code $ref} switching it shares: replacing an
 * existing key keeps its position, so the links never reorder.
 */
public final class ResponseLinks {

    private static final String FIELD = "links";

    private final ObjectNode parent;

    ResponseLinks(ObjectNode parent) {
        this.parent = parent;
    }

    /** Every key, including one whose value is not an object, so the file is never hidden from the user. */
    public List<String> names() {
        List<String> names = new ArrayList<>();
        if (parent.get(FIELD) instanceof ObjectNode links) {
            Iterator<String> it = links.fieldNames();
            while (it.hasNext()) {
                names.add(it.next());
            }
        }
        return names;
    }

    /** The named link, or {@code null} if it is absent or not an object. */
    public Link get(String name) {
        return parent.get(FIELD) instanceof ObjectNode links && links.get(name) instanceof ObjectNode link
                ? new Link(link)
                : null;
    }

    /** Adds an empty inline link, or returns the existing one of that name. */
    public Link add(String name) {
        return new Link(JsonNodes.objectChild(JsonNodes.objectChild(parent, FIELD), name));
    }

    public void remove(String name) {
        if (parent.get(FIELD) instanceof ObjectNode links) {
            links.remove(name);
            if (links.isEmpty()) {
                parent.remove(FIELD);
            }
        }
    }

    /** In place, so the saved file is not reordered; {@code false} means refused. See {@link JsonNodes#renameField}. */
    public boolean rename(String from, String to) {
        return parent.get(FIELD) instanceof ObjectNode links && JsonNodes.renameField(links, from, to);
    }

    /**
     * Makes the named entry a reference to a component link, replacing whatever was there — an
     * inline link included, so a caller that cares asks first (see {@link Link#isEmpty}).
     */
    public Link referTo(String name, String componentName) {
        ObjectNode ref = JsonNodeFactory.instance.objectNode();
        ref.put("$ref", ComponentReferences.ref(ComponentLinks.SECTION, componentName));
        JsonNodes.objectChild(parent, FIELD).set(name, ref);
        return new Link(ref);
    }

    /** Makes the named entry inline, in place, from a copy of {@code template} (what the reference pointed at). */
    public Link defineInline(String name, Link template) {
        ObjectNode inline = template != null ? template.node().deepCopy() : JsonNodeFactory.instance.objectNode();
        JsonNodes.objectChild(parent, FIELD).set(name, inline);
        return new Link(inline);
    }
}
