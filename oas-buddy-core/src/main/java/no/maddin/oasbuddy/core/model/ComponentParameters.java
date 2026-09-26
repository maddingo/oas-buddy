package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;
import no.maddin.oasbuddy.core.document.LazyObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The named map at {@code components.parameters}: a parameter defined once and referenced from any
 * number of operations or paths. The key is how references name it; the parameter's own
 * {@code name} is what goes on the wire, and the two need not match.
 */
public final class ComponentParameters {

    /** The {@code components} field these live in, as {@link ComponentReferences} names it. */
    public static final String SECTION = "parameters";

    private final LazyObjectNode node;

    public ComponentParameters(LazyObjectNode node) {
        this.node = node;
    }

    public List<String> names() {
        ObjectNode existing = node.peek();
        if (existing == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        Iterator<String> it = existing.fieldNames();
        while (it.hasNext()) {
            names.add(it.next());
        }
        return names;
    }

    public Parameter getParameter(String key) {
        ObjectNode existing = node.peek();
        return existing != null && existing.get(key) instanceof ObjectNode parameterNode
                ? new Parameter(parameterNode)
                : null;
    }

    /** Adds a parameter under {@code key}, with {@code name} and {@code in} filled in. */
    public Parameter addParameter(String key, String name, String in) {
        Parameter parameter = new Parameter(JsonNodes.objectChild(node.create(), key));
        parameter.setName(name);
        parameter.setIn(in);
        return parameter;
    }

    public void removeParameter(String key) {
        ObjectNode existing = node.peek();
        if (existing != null) {
            existing.remove(key);
        }
    }
}
