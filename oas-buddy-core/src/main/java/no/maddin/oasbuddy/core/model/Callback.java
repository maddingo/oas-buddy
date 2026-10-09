package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A Callback object: a map of runtime expressions (e.g. {@code {$request.body#/callbackUrl}}) to the
 * {@link PathItem} the API will call, or a {@code $ref} to one in {@code components.callbacks}.
 *
 * <p>A {@code x-} extension sits in the same map as the expressions in the file but is not one, so it
 * is neither listed nor touched here.
 */
public final class Callback {

    private static final String REF = "$ref";

    private final ObjectNode node;

    public Callback(ObjectNode node) {
        this.node = node;
    }

    ObjectNode node() {
        return node;
    }

    public String getRef() {
        return JsonNodes.text(node, REF);
    }

    /** The component callback this refers to, or {@code null} if it is inline (or refers elsewhere). */
    public String getReferencedCallbackName() {
        return ComponentReferences.nameOf(ComponentCallbacks.SECTION, getRef());
    }

    public boolean isReference() {
        return node.has(REF);
    }

    /** Whether there is nothing to lose: no field at all. */
    public boolean isEmpty() {
        return node.isEmpty();
    }

    /** Every expression key, including one whose value is not an object, so the file is never hidden. */
    public List<String> expressions() {
        List<String> names = new ArrayList<>();
        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            String name = it.next();
            if (!isExpression(name)) {
                continue;
            }
            names.add(name);
        }
        return names;
    }

    /** The path item for {@code expression}, or {@code null} if it is absent or not an object. */
    public PathItem get(String expression) {
        return isExpression(expression) && node.get(expression) instanceof ObjectNode item ? new PathItem(item) : null;
    }

    /** Adds an empty path item, or returns the existing one. */
    public PathItem add(String expression) {
        return new PathItem(JsonNodes.objectChild(node, expression));
    }

    public void remove(String expression) {
        if (isExpression(expression)) {
            node.remove(expression);
        }
    }

    /** In place, so the saved file is not reordered; {@code false} means refused. See {@link JsonNodes#renameField}. */
    public boolean rename(String from, String to) {
        return isExpression(from) && isExpression(to) && JsonNodes.renameField(node, from, to);
    }

    private static boolean isExpression(String key) {
        return key != null && !REF.equals(key) && !key.startsWith("x-");
    }
}
