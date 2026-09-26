package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The {@code content} map of a request body or response: media type → {@link MediaType}, in
 * document order.
 *
 * <p>Takes the parent and reads lazily, so showing a response never grows a {@code content} it did
 * not have. Removing the last media type takes the {@code content} key out with it rather than
 * leaving a {@code content: {}} husk: for a response that is exactly the valid, content-less form
 * (a {@code 204}, say). A request body without {@code content} is not valid OAS 3.0 — {@code content}
 * is required there — and that is deliberately left for validation to report rather than kept as
 * an empty object that only looks more valid.
 */
public final class Content {

    private static final String FIELD = "content";

    private final ObjectNode parent;

    public Content(ObjectNode parent) {
        this.parent = parent;
    }

    public List<String> mediaTypes() {
        List<String> names = new ArrayList<>();
        if (parent.get(FIELD) instanceof ObjectNode content) {
            Iterator<String> it = content.fieldNames();
            while (it.hasNext()) {
                names.add(it.next());
            }
        }
        return names;
    }

    /** The media type if present, otherwise {@code null}. Reads only. */
    public MediaType get(String mediaType) {
        return parent.get(FIELD) instanceof ObjectNode content
                && content.get(mediaType) instanceof ObjectNode mediaTypeNode
                ? new MediaType(mediaTypeNode)
                : null;
    }

    /** The media type, adding it (at the end) and {@code content} if absent. */
    public MediaType add(String mediaType) {
        return new MediaType(JsonNodes.objectChild(JsonNodes.objectChild(parent, FIELD), mediaType));
    }

    public void remove(String mediaType) {
        if (parent.get(FIELD) instanceof ObjectNode content) {
            content.remove(mediaType);
            if (content.isEmpty()) {
                parent.remove(FIELD);
            }
        }
    }

    /**
     * Renames a media type in place, keeping its position and everything under it.
     *
     * @return {@code false}, changing nothing, on a blank, absent, unchanged or colliding name
     */
    public boolean rename(String from, String to) {
        return parent.get(FIELD) instanceof ObjectNode content && JsonNodes.renameField(content, from, to);
    }
}
