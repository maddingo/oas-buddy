package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A media type's named {@code examples}. Reads lazily and takes the key out with the last entry,
 * like {@link Content}; switching an entry between inline and a reference replaces it in place,
 * like {@link Responses#referTo}.
 */
public final class MediaTypeExamples {

    private static final String FIELD = "examples";

    private final ObjectNode parent;

    MediaTypeExamples(ObjectNode parent) {
        this.parent = parent;
    }

    public List<String> names() {
        List<String> names = new ArrayList<>();
        if (parent.get(FIELD) instanceof ObjectNode examples) {
            Iterator<String> it = examples.fieldNames();
            while (it.hasNext()) {
                names.add(it.next());
            }
        }
        return names;
    }

    public Example get(String name) {
        return parent.get(FIELD) instanceof ObjectNode examples && examples.get(name) instanceof ObjectNode example
                ? new Example(example)
                : null;
    }

    /** Adds an empty inline example, or returns the existing one of that name. */
    public Example add(String name) {
        return new Example(JsonNodes.objectChild(JsonNodes.objectChild(parent, FIELD), name));
    }

    public void remove(String name) {
        if (parent.get(FIELD) instanceof ObjectNode examples) {
            examples.remove(name);
            if (examples.isEmpty()) {
                parent.remove(FIELD);
            }
        }
    }

    /**
     * Makes the named entry a reference to a component example, replacing whatever was there — an
     * inline example included, so a caller that cares asks first (see {@link Example#isEmpty}).
     * Added at the end if the name is new.
     */
    public Example referTo(String name, String componentName) {
        ObjectNode ref = JsonNodeFactory.instance.objectNode();
        ref.put("$ref", ComponentReferences.ref(ComponentExamples.SECTION, componentName));
        JsonNodes.objectChild(parent, FIELD).set(name, ref);
        return new Example(ref);
    }

    /**
     * Makes the named entry inline, in place, starting from a copy of {@code template} — the
     * example the reference pointed at — or empty without one.
     */
    public Example defineInline(String name, Example template) {
        ObjectNode inline = template != null ? template.node().deepCopy() : JsonNodeFactory.instance.objectNode();
        JsonNodes.objectChild(parent, FIELD).set(name, inline);
        return new Example(inline);
    }
}
