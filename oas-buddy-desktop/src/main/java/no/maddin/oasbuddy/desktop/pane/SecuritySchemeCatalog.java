package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.SecurityScheme;

import java.util.List;

/**
 * What a requirement may point at: the schemes a document declares, and the scopes each of them
 * offers.
 *
 * <p>An interface rather than the document itself so {@link OperationPane} keeps knowing only about
 * its own operation — it has never taken an {@link OasDocument} and does not need to start.
 */
public interface SecuritySchemeCatalog {

    /** The declared scheme names, in document order. */
    List<String> schemeNames();

    /** The scopes a requirement on that scheme may ask for; empty unless the scheme is oauth2. */
    List<String> scopesOf(String schemeName);

    /** Reads both straight off the document, so the catalog is never stale. */
    static SecuritySchemeCatalog of(OasDocument document) {
        return new SecuritySchemeCatalog() {
            @Override
            public List<String> schemeNames() {
                return document.getComponents().getSecuritySchemes().names();
            }

            @Override
            public List<String> scopesOf(String schemeName) {
                SecurityScheme scheme = document.getComponents().getSecuritySchemes().getScheme(schemeName);
                return scheme == null ? List.of() : scheme.declaredScopes();
            }
        };
    }
}
