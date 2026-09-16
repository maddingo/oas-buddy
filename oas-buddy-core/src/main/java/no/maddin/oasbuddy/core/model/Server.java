package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

public final class Server {

    private final ObjectNode node;

    public Server(ObjectNode node) {
        this.node = node;
    }

    public String getUrl() {
        return JsonNodes.text(node, "url");
    }

    public void setUrl(String url) {
        JsonNodes.setText(node, "url", url);
    }

    public String getDescription() {
        return JsonNodes.text(node, "description");
    }

    public void setDescription(String description) {
        JsonNodes.setText(node, "description", description);
    }
}
