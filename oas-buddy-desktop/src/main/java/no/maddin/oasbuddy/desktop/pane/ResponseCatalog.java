package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.ApiResponse;

import java.util.List;

/**
 * What an operation's response may refer to: the responses declared under
 * {@code components.responses}. An interface for the same reason as {@link SecuritySchemeCatalog} —
 * {@link OperationPane} keeps knowing only about its own operation.
 */
public interface ResponseCatalog {

    /** The declared response names, in document order. */
    List<String> responseNames();

    /** The declared response, or {@code null} if there is none by that name. */
    ApiResponse response(String name);

    /** Reads straight off the document, so the catalog is never stale. */
    static ResponseCatalog of(OasDocument document) {
        return new ResponseCatalog() {
            @Override
            public List<String> responseNames() {
                return document.getComponents().getResponses().names();
            }

            @Override
            public ApiResponse response(String name) {
                return document.getComponents().getResponses().getResponse(name);
            }
        };
    }
}
