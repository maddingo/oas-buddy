package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import no.maddin.oasbuddy.core.model.Operation;
import no.maddin.oasbuddy.core.model.PathItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Removing a path or a single operation, shared by the path list, the path editor and the
 * operation editor.
 */
public final class PathRemoval {

    private PathRemoval() {
    }

    public static void removePath(OasDocument document, String path,
                                  RemovalConfirmation confirmation, Runnable onRemoved) {
        PathItem pathItem = document.getPaths().getPathItem(path);
        if (pathItem == null) {
            return;
        }
        if (!confirmation.confirm("Remove path \"" + path + "\"?", describeOperations(pathItem))) {
            return;
        }
        document.getPaths().removePath(path);
        onRemoved.run();
    }

    public static void removeOperation(OasDocument document, String path, HttpMethod method,
                                       RemovalConfirmation confirmation, Runnable onRemoved) {
        PathItem pathItem = document.getPaths().getPathItem(path);
        Operation operation = pathItem == null ? null : pathItem.getOperation(method);
        if (operation == null) {
            return;
        }
        if (!confirmation.confirm("Remove " + method.name() + " " + path + "?", describe(operation))) {
            return;
        }
        pathItem.removeOperation(method);
        onRemoved.run();
    }

    /** The dialog's body text for a path: the operations that would go with it. */
    static String describeOperations(PathItem pathItem) {
        Map<HttpMethod, Operation> operations = pathItem.getOperations();
        if (operations.isEmpty()) {
            return "It has no operations.";
        }
        List<String> lines = new ArrayList<>();
        operations.forEach((method, operation) -> lines.add(label(method, operation)));
        return "It also removes these operations:\n  " + String.join("\n  ", lines);
    }

    /** How one operation is named in the UI: its id and summary, as far as either is set. */
    static String describe(Operation operation) {
        List<String> parts = new ArrayList<>();
        if (operation.getOperationId() != null) {
            parts.add(operation.getOperationId());
        }
        if (operation.getSummary() != null) {
            parts.add(operation.getSummary());
        }
        return String.join(" — ", parts);
    }

    /** How an operation appears in a list: the method plus its id, or its summary as a fallback. */
    static String label(HttpMethod method, Operation operation) {
        String name = operation.getOperationId() != null ? operation.getOperationId() : operation.getSummary();
        return name == null || name.isBlank() ? method.name() : method.name() + " — " + name;
    }
}
