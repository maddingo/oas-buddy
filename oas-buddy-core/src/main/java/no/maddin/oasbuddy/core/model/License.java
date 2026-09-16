package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

public final class License {

    private final ObjectNode node;

    public License(ObjectNode node) {
        this.node = node;
    }

    public String getName() {
        return JsonNodes.text(node, "name");
    }

    public void setName(String name) {
        JsonNodes.setText(node, "name", name);
    }

    public String getUrl() {
        return JsonNodes.text(node, "url");
    }

    public void setUrl(String url) {
        JsonNodes.setText(node, "url", url);
    }
}
