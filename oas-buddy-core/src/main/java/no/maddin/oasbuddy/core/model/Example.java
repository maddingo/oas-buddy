package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

/**
 * An Example object — {@code summary}, {@code description}, {@code value}, {@code externalValue} —
 * or a {@code $ref} to one in {@code components.examples}.
 */
public final class Example {

    private static final String REF = "$ref";
    private static final String VALUE = "value";

    private final ObjectNode node;

    public Example(ObjectNode node) {
        this.node = node;
    }

    ObjectNode node() {
        return node;
    }

    public String getRef() {
        return JsonNodes.text(node, REF);
    }

    /** The component example this refers to, or {@code null} if it is inline (or refers elsewhere). */
    public String getReferencedExampleName() {
        return ComponentReferences.nameOf(ComponentExamples.SECTION, getRef());
    }

    public boolean isReference() {
        return node.has(REF);
    }

    /** Whether there is nothing to lose: no field at all. */
    public boolean isEmpty() {
        return node.isEmpty();
    }

    public String getSummary() {
        return JsonNodes.text(node, "summary");
    }

    public void setSummary(String summary) {
        JsonNodes.setText(node, "summary", summary);
    }

    public String getDescription() {
        return JsonNodes.text(node, "description");
    }

    public void setDescription(String description) {
        JsonNodes.setText(node, "description", description);
    }

    public JsonNode getValue() {
        return node.get(VALUE);
    }

    /** {@code null} removes the key. */
    public void setValue(JsonNode value) {
        if (value == null) {
            node.remove(VALUE);
        } else {
            node.set(VALUE, value);
        }
    }

    public String getExternalValue() {
        return JsonNodes.text(node, "externalValue");
    }

    public void setExternalValue(String externalValue) {
        JsonNodes.setText(node, "externalValue", externalValue);
    }
}
