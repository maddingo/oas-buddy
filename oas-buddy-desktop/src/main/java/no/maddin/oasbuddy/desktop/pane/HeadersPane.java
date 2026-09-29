package no.maddin.oasbuddy.desktop.pane;

import atlantafx.base.theme.Styles;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.ComponentHeaders;
import no.maddin.oasbuddy.core.model.Header;

import java.util.List;
import java.util.function.Consumer;

/** The list of headers under {@code components.headers}, the counterpart to {@link ExamplesPane}. */
public final class HeadersPane {

    private HeadersPane() {
    }

    /**
     * @param onStructureChanged run after a header is added, so the outline picks it up
     * @param onRemoveHeader     asked to remove the named header; the pane only reports the
     *                           request, it never removes anything itself
     */
    public static Node build(OasDocument document, Runnable onStructureChanged, Consumer<String> onRemoveHeader) {
        ComponentHeaders headers = document.getComponents().getHeaders();

        GridPane list = FormFields.grid();
        list.setId("headers-list");
        ColumnConstraints nameColumn = FormFields.column(HPos.LEFT, Priority.NEVER);
        nameColumn.setMinWidth(220);
        list.getColumnConstraints().addAll(nameColumn,
                FormFields.column(HPos.LEFT, Priority.NEVER),
                FormFields.column(HPos.LEFT, Priority.NEVER));
        fillList(headers, list, onRemoveHeader);

        TextField nameField = new TextField();
        nameField.setId("new-header-name");
        nameField.setPromptText("X-Rate-Limit");
        Button addButton = new Button("Add header");
        addButton.setId("add-component-header");
        addButton.getStyleClass().add(Styles.ACCENT);
        addButton.setOnAction(e -> {
            String name = nameField.getText();
            if (name != null && !name.isBlank()) {
                headers.addHeader(name.strip());
                nameField.clear();
                onStructureChanged.run();
            }
        });

        return FormFields.root(FormFields.heading("Headers"), list, new HBox(8, nameField, addButton));
    }

    private static void fillList(ComponentHeaders headers, GridPane list, Consumer<String> onRemoveHeader) {
        List<String> names = headers.names();
        if (names.isEmpty()) {
            Label empty = new Label("No reusable headers yet.");
            empty.getStyleClass().add(Styles.TEXT_MUTED);
            list.add(empty, 0, 0, 3, 1);
            return;
        }

        list.addRow(0, FormFields.columnHeading("Header"), FormFields.columnHeading("Description"));

        int row = 1;
        for (String name : names) {
            Header header = headers.getHeader(name);
            Label description;
            if (header == null) {
                description = FormFields.notAnObject();
            } else {
                description = new Label(header.getDescription() == null ? "" : header.getDescription());
                description.getStyleClass().add(Styles.TEXT_MUTED);
            }

            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
            removeButton.setOnAction(e -> onRemoveHeader.accept(name));

            list.addRow(row++, new Label(name), description, removeButton);
        }
    }
}
