package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * One entry of a {@code security} array: the schemes that must all be satisfied together, each with
 * the scopes it requires.
 *
 * <p>Entries of the array are alternatives (any one of them is enough); the schemes named inside a
 * single entry are required at once. The editor only builds single-scheme entries, but a combined
 * one loaded from a file has to be recognisable so it can be shown without being offered for
 * editing — see {@link #isCombined()}.
 */
public final class SecurityRequirement {

    private final ObjectNode node;

    public SecurityRequirement(ObjectNode node) {
        this.node = node;
    }

    /** The schemes this requirement names, in document order. */
    public List<String> schemeNames() {
        List<String> names = new ArrayList<>();
        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            names.add(it.next());
        }
        return names;
    }

    /** Whether this requirement names several schemes, i.e. demands all of them at once. */
    public boolean isCombined() {
        return node.size() > 1;
    }

    /** The scopes required of the named scheme; empty for a scheme that has none. */
    public List<String> getScopes(String schemeName) {
        List<String> scopes = new ArrayList<>();
        if (node.get(schemeName) instanceof ArrayNode array) {
            for (var element : array) {
                scopes.add(element.asText());
            }
        }
        return scopes;
    }

    /**
     * Replaces the scopes required of the named scheme. An empty list leaves the scheme required
     * with an empty array, which is what a non-oauth2 scheme must carry — it does not un-require it.
     */
    public void setScopes(String schemeName, List<String> scopes) {
        ArrayNode array = JsonNodes.arrayChild(node, schemeName);
        array.removeAll();
        scopes.forEach(array::add);
    }
}
