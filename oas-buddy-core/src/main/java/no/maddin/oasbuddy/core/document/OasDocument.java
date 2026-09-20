package no.maddin.oasbuddy.core.document;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.model.Components;
import no.maddin.oasbuddy.core.model.Info;
import no.maddin.oasbuddy.core.model.Paths;
import no.maddin.oasbuddy.core.model.Server;

import java.util.ArrayList;
import java.util.List;

/**
 * Facade over an OpenAPI document tree. The tree (a Jackson {@link ObjectNode}) is the
 * single source of truth; this class and the types in {@code no.maddin.oasbuddy.core.model} only
 * provide typed read/write access to it, so edits always round-trip back to the original
 * key order.
 */
public final class OasDocument {

    private final ObjectNode root;
    private final DocumentFormat format;

    private OasDocument(ObjectNode root, DocumentFormat format) {
        this.root = root;
        this.format = format;
    }

    public static OasDocument wrap(ObjectNode root, DocumentFormat format) {
        return new OasDocument(root, format);
    }

    public static OasDocument newDocument(DocumentFormat format) {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("openapi", "3.0.3");
        ObjectNode info = JsonNodes.objectChild(root, "info");
        info.put("title", "New API");
        info.put("version", "0.1.0");
        JsonNodes.objectChild(root, "paths");
        return new OasDocument(root, format);
    }

    public ObjectNode getRoot() {
        return root;
    }

    public DocumentFormat getFormat() {
        return format;
    }

    public String getOpenApiVersion() {
        return JsonNodes.text(root, "openapi");
    }

    public void setOpenApiVersion(String version) {
        JsonNodes.setText(root, "openapi", version);
    }

    public Info getInfo() {
        return new Info(JsonNodes.objectChild(root, "info"));
    }

    public List<Server> getServers() {
        var array = JsonNodes.arrayChild(root, "servers");
        List<Server> servers = new ArrayList<>();
        for (var element : array) {
            if (element instanceof ObjectNode objectNode) {
                servers.add(new Server(objectNode));
            }
        }
        return servers;
    }

    public Server addServer(String url) {
        var array = JsonNodes.arrayChild(root, "servers");
        ObjectNode node = array.addObject();
        node.put("url", url);
        return new Server(node);
    }

    public void removeServer(int index) {
        JsonNodes.arrayChild(root, "servers").remove(index);
    }

    public Paths getPaths() {
        return new Paths(JsonNodes.objectChild(root, "paths"));
    }

    /**
     * The components section, resolved lazily: it is added to the document only if something is
     * actually written to it. See {@link LazyObjectNode}.
     */
    public Components getComponents() {
        return new Components(LazyObjectNode.of(root, "components"));
    }
}
