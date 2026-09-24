package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;

/**
 * Renaming a path. Nothing can {@code $ref} a path, so unlike {@link SchemaRename} there is
 * nothing else in the document to fix up or to ask about first — a collision is the only reason
 * this refuses, and {@code Paths.renamePath} already reports that.
 */
public final class PathRename {

    private PathRename() {
    }

    /** @return whether the rename went ahead */
    public static boolean rename(OasDocument document, String from, String to, Runnable onRenamed) {
        if (!document.getPaths().renamePath(from, to)) {
            return false;
        }
        onRenamed.run();
        return true;
    }
}
