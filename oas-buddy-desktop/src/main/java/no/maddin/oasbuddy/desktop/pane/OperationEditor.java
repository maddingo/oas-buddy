package no.maddin.oasbuddy.desktop.pane;

import javafx.scene.Node;
import no.maddin.oasbuddy.core.model.Operation;

/**
 * Builds the editor for one operation. Lets a callback's nested path item edit its operations with
 * the very pane a top-level operation gets, without the callbacks editor having to know the schema,
 * tag and security catalogs that pane needs — and without the two classes depending on each other.
 */
@FunctionalInterface
interface OperationEditor {

    /** @param onRemove run when the editor's own Delete button is pressed; it only reports the request */
    Node create(Operation operation, Runnable onRemove);
}
