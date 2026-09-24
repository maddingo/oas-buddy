package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;

import java.util.List;

/**
 * What an operation's tag picker may offer, and where a tag typed inline ends up.
 *
 * <p>An interface rather than the document itself, the same reason {@link SecuritySchemeCatalog}
 * is one: {@link OperationPane} keeps knowing only about its own operation, never an
 * {@link OasDocument}.
 */
public interface TagCatalog {

    /** The declared tag names, in document order. */
    List<String> tagNames();

    /** Declares this name at the root if it is not already there; a no-op if it is. */
    void ensureDeclared(String name);

    /** Reads straight off the document, so the catalog is never stale. */
    static TagCatalog of(OasDocument document) {
        return new TagCatalog() {
            @Override
            public List<String> tagNames() {
                return document.getTags().names();
            }

            @Override
            public void ensureDeclared(String name) {
                document.getTags().add(name);
            }
        };
    }
}
