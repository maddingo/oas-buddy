package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Schema {

    private final ObjectNode node;

    public Schema(ObjectNode node) {
        this.node = node;
    }

    public String getRef() {
        return JsonNodes.text(node, "$ref");
    }

    public void setRef(String ref) {
        JsonNodes.setText(node, "$ref", ref);
    }

    public String getType() {
        return JsonNodes.text(node, "type");
    }

    public void setType(String type) {
        JsonNodes.setText(node, "type", type);
    }

    public String getFormat() {
        return JsonNodes.text(node, "format");
    }

    public void setFormat(String format) {
        JsonNodes.setText(node, "format", format);
    }

    public String getDescription() {
        return JsonNodes.text(node, "description");
    }

    public void setDescription(String description) {
        JsonNodes.setText(node, "description", description);
    }

    public List<String> getRequired() {
        List<String> required = new ArrayList<>();
        var existing = node.get("required");
        if (existing instanceof ArrayNode arrayNode) {
            for (var element : arrayNode) {
                required.add(element.asText());
            }
        }
        return required;
    }

    public void setRequired(List<String> required) {
        ArrayNode array = JsonNodes.arrayChild(node, "required");
        array.removeAll();
        for (String name : required) {
            array.add(name);
        }
    }

    public List<String> propertyNames() {
        List<String> names = new ArrayList<>();
        var existing = node.get("properties");
        if (existing instanceof ObjectNode objectNode) {
            Iterator<String> it = objectNode.fieldNames();
            while (it.hasNext()) {
                names.add(it.next());
            }
        }
        return names;
    }

    public Schema getProperty(String name) {
        var properties = node.get("properties");
        if (!(properties instanceof ObjectNode objectNode)) {
            return null;
        }
        var existing = objectNode.get(name);
        return existing instanceof ObjectNode propertyNode ? new Schema(propertyNode) : null;
    }

    public Schema addProperty(String name) {
        ObjectNode properties = JsonNodes.objectChild(node, "properties");
        return new Schema(JsonNodes.objectChild(properties, name));
    }

    public void removeProperty(String name) {
        var properties = node.get("properties");
        if (properties instanceof ObjectNode objectNode) {
            objectNode.remove(name);
        }
    }

    public Schema getItems() {
        return new Schema(JsonNodes.objectChild(node, "items"));
    }
}
