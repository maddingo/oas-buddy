package no.maddin.oasbuddy.core.model;

import no.maddin.oasbuddy.core.document.OasDocument;

import java.util.ArrayList;
import java.util.List;

/** The {@code operationId}s the document's paths declare, for anything that has to point at one. */
public final class OperationIds {

    private OperationIds() {
    }

    /** Non-blank ids in document order; reads only. A path or operation that is not an object has none. */
    public static List<String> all(OasDocument document) {
        List<String> ids = new ArrayList<>();
        for (String path : document.getPaths().pathNames()) {
            PathItem pathItem = document.getPaths().getPathItem(path);
            if (pathItem == null) {
                continue;
            }
            for (Operation operation : pathItem.getOperations().values()) {
                String id = operation.getOperationId();
                if (id != null && !id.isBlank()) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }
}
