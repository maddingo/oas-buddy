package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.SecurityScheme;
import no.maddin.oasbuddy.core.model.SecuritySchemes;
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

import java.util.List;
import java.util.function.Consumer;

public final class SecuritySchemesPane {

    /** What a scheme starts out as; the type picker in the editor changes it from there. */
    private static final String DEFAULT_TYPE = "apiKey";

    private SecuritySchemesPane() {
    }

    /**
     * @param onStructureChanged run after a scheme is added, so the outline picks it up
     * @param onRemoveScheme     asked to remove the named scheme; the pane only reports the
     *                           request, it never removes anything itself
     */
    public static Node build(OasDocument document, Runnable onStructureChanged, Consumer<String> onRemoveScheme) {
        SecuritySchemes schemes = document.getComponents().getSecuritySchemes();

        GridPane list = FormFields.grid();
        list.setId("security-schemes-list");
        // the name column keeps a minimum width so Type and Remove stay next to the names
        // instead of being pushed to the far edge of a wide window
        ColumnConstraints nameColumn = FormFields.column(HPos.LEFT, Priority.NEVER);
        nameColumn.setMinWidth(220);
        list.getColumnConstraints().addAll(nameColumn,
                FormFields.column(HPos.LEFT, Priority.NEVER),
                FormFields.column(HPos.LEFT, Priority.NEVER));
        fillList(schemes, list, onRemoveScheme);

        TextField nameField = new TextField();
        nameField.setId("new-security-scheme-name");
        nameField.setPromptText("ApiKeyAuth");
        Button addButton = new Button("Add scheme");
        addButton.setId("add-security-scheme");
        addButton.getStyleClass().add(Styles.ACCENT);
        addButton.setOnAction(e -> {
            String name = nameField.getText();
            if (name != null && !name.isBlank()) {
                schemes.addScheme(name.strip()).setType(DEFAULT_TYPE);
                nameField.clear();
                onStructureChanged.run();
            }
        });

        return FormFields.root(FormFields.heading("Security schemes"), list, new HBox(8, nameField, addButton));
    }

    private static void fillList(SecuritySchemes schemes, GridPane list, Consumer<String> onRemoveScheme) {
        List<String> names = schemes.names();
        if (names.isEmpty()) {
            Label empty = new Label("No security schemes yet.");
            empty.getStyleClass().add(Styles.TEXT_MUTED);
            list.add(empty, 0, 0, 3, 1);
            return;
        }

        list.addRow(0, FormFields.columnHeading("Scheme"), FormFields.columnHeading("Type"));

        int row = 1;
        for (String name : names) {
            SecurityScheme scheme = schemes.getScheme(name);

            Label type = new Label(scheme.getType() == null ? "" : scheme.getType());
            if (!scheme.isEditable()) {
                // the editor has no form for this type; say so here rather than only on the pane
                type.getStyleClass().add(Styles.TEXT_MUTED);
            }

            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
            removeButton.setOnAction(e -> onRemoveScheme.accept(name));

            list.addRow(row++, new Label(name), type, removeButton);
        }
    }
}
