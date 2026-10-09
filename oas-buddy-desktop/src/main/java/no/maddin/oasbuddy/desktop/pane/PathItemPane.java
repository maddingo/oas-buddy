package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import no.maddin.oasbuddy.core.model.Operation;
import no.maddin.oasbuddy.core.model.PathItem;
import atlantafx.base.theme.Styles;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class PathItemPane {

    static final String OPERATION_CLASS = "nested-operation";
    static final String ADD_OPERATION_CLASS = "add-nested-operation";

    private PathItemPane() {
    }

    /**
     * @param onStructureChanged  run after an operation is added, so the outline picks it up
     * @param onRenamePath        asked to rename this path to a new, not-yet-used one; returns
     *                            whether the rename went ahead. The pane only reports the request,
     *                            it never renames anything itself.
     * @param confirmation        asked before an inline path parameter is replaced by a reference
     * @param onRemovePath        asked to remove this whole path
     * @param onRemoveOperation   asked to remove one of its operations; like the other panes this
     *                            one only reports the request, it never removes anything itself
     */
    public static Node build(OasDocument document, String path, Runnable onStructureChanged,
                             BiFunction<String, String, Boolean> onRenamePath,
                             RemovalConfirmation confirmation, Runnable onRemovePath, Consumer<HttpMethod> onRemoveOperation) {
        PathItem pathItem = document.getPaths().getPathItem(path);

        TextField nameField = FormFields.renameField(path, candidate ->
                document.getPaths().pathNames().contains(candidate)
                        ? "A path named \"" + candidate + "\" already exists."
                        : onRenamePath.apply(path, candidate) ? null : "");
        nameField.setId("path-name");
        if (pathItem == null) {
            return FormFields.root(
                    FormFields.headerWithRenameAndDelete("Path", nameField, "delete-path", "Delete path", onRemovePath),
                    FormFields.notEditable(path, "a path item"));
        }

        List<Node> fields = fields(pathItem, ComponentCatalog.of(document), confirmation);

        GridPane operations = FormFields.grid();
        operations.setId("path-operations");
        ColumnConstraints operationColumn = FormFields.column(HPos.LEFT, Priority.NEVER);
        operationColumn.setMinWidth(260);
        operations.getColumnConstraints().addAll(operationColumn, FormFields.column(HPos.LEFT, Priority.NEVER));
        fillOperations(pathItem, operations, onRemoveOperation);

        FlowPane methodButtons = new FlowPane(8, 8);
        var existing = pathItem.getOperations().keySet();
        for (HttpMethod method : HttpMethod.values()) {
            if (!existing.contains(method)) {
                Button addButton = new Button("+ " + method.name());
                addButton.getStyleClass().addAll(Styles.SMALL, Styles.BUTTON_OUTLINED);
                addButton.setOnAction(e -> {
                    pathItem.addOperation(method);
                    onStructureChanged.run();
                });
                methodButtons.getChildren().add(addButton);
            }
        }

        Label addOperationLabel = new Label("Add operation");
        addOperationLabel.getStyleClass().add(Styles.TEXT_MUTED);

        List<Node> sections = new ArrayList<>();
        sections.add(FormFields.headerWithRenameAndDelete("Path", nameField, "delete-path", "Delete path", onRemovePath));
        sections.addAll(fields);
        sections.addAll(List.of(FormFields.heading("Operations"), operations, addOperationLabel, methodButtons));
        return FormFields.root(sections.toArray(Node[]::new));
    }

    /**
     * A path item that lives inside another node — a callback's expression — rather than under
     * {@code paths}: the same summary, description, parameters and servers as a top-level path, and
     * its operations edited in place with {@code operationEditor} instead of as outline children.
     *
     * @param onChanged        run after an operation is added or removed, so the caller rebuilds this view
     * @param operationEditor  builds the editor for one of its operations
     */
    static Node embedded(PathItem pathItem, ComponentCatalog catalog, RemovalConfirmation confirmation,
                         OperationEditor operationEditor, Runnable onChanged) {
        List<Node> sections = new ArrayList<>(fields(pathItem, catalog, confirmation));
        sections.add(FormFields.heading("Operations"));
        for (var entry : pathItem.getOperations().entrySet()) {
            HttpMethod method = entry.getKey();
            Operation operation = entry.getValue();
            Node editor = operationEditor.create(operation, () -> {
                if (confirmation.confirm("Remove " + method.name() + "?", PathRemoval.describe(operation))) {
                    pathItem.removeOperation(method);
                    onChanged.run();
                }
            });
            TitledPane pane = new TitledPane(PathRemoval.label(method, operation), editor);
            pane.getStyleClass().add(OPERATION_CLASS);
            pane.setExpanded(false);
            sections.add(pane);
        }
        if (pathItem.getOperations().isEmpty()) {
            Label empty = new Label("No operations yet.");
            empty.getStyleClass().add(Styles.TEXT_MUTED);
            sections.add(empty);
        }
        FlowPane methodButtons = new FlowPane(8, 8);
        for (HttpMethod method : HttpMethod.values()) {
            if (!pathItem.getOperations().containsKey(method)) {
                Button addButton = new Button("+ " + method.name());
                addButton.getStyleClass().addAll(Styles.SMALL, Styles.BUTTON_OUTLINED, ADD_OPERATION_CLASS);
                addButton.setOnAction(e -> {
                    pathItem.addOperation(method);
                    onChanged.run();
                });
                methodButtons.getChildren().add(addButton);
            }
        }
        sections.add(methodButtons);
        VBox box = new VBox(8, sections.toArray(Node[]::new));
        return box;
    }

    /** Summary, description, parameters and servers: what a path item has whether or not it sits under {@code paths}. */
    private static List<Node> fields(PathItem pathItem, ComponentCatalog catalog, RemovalConfirmation confirmation) {
        GridPane grid = FormFields.grid();
        int row = 0;
        FormFields.textRow(grid, row++, "Summary", pathItem::getSummary, pathItem::setSummary);
        FormFields.textAreaRow(grid, row, "Description", pathItem::getDescription, pathItem::setDescription);

        // declared once here, these apply to every operation on the path
        VBox parameters = ParametersEditor.build(pathItem.getParameters(), catalog,
                confirmation, "path-parameters");
        Label parametersNote = new Label("Apply to every operation on this path.");
        parametersNote.getStyleClass().add(Styles.TEXT_MUTED);

        return List.of(grid,
                FormFields.heading("Parameters"), parametersNote, parameters,
                FormFields.heading("Servers"), ServersEditor.override(pathItem.getServers(), confirmation, "path"));
    }

    private static void fillOperations(PathItem pathItem, GridPane operations,
                                       Consumer<HttpMethod> onRemoveOperation) {
        Map<HttpMethod, Operation> existing = pathItem.getOperations();
        if (existing.isEmpty()) {
            Label empty = new Label("No operations yet.");
            empty.getStyleClass().add(Styles.TEXT_MUTED);
            operations.add(empty, 0, 0, 2, 1);
            return;
        }

        operations.addRow(0, FormFields.columnHeading("Operation"));

        int row = 1;
        for (var entry : existing.entrySet()) {
            HttpMethod method = entry.getKey();
            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
            removeButton.setOnAction(e -> onRemoveOperation.accept(method));
            operations.addRow(row++, new Label(PathRemoval.label(method, entry.getValue())), removeButton);
        }
    }
}
