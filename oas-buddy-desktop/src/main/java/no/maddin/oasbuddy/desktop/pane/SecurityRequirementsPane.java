package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.SecurityRequirement;
import no.maddin.oasbuddy.core.model.SecurityRequirements;
import atlantafx.base.theme.Styles;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of security requirements, shared by the document-level security pane and the operation
 * pane.
 *
 * <p>Each row is one alternative: satisfying any single row is enough. A row names one scheme and,
 * when that scheme is oauth2, the scopes it requires. A row naming several schemes demands all of
 * them at once — this editor does not build those, so such a row is shown with its schemes named and
 * no editing controls, only a Remove button. Leaving it invisible would mean showing the user a
 * security list that disagrees with their file.
 */
public final class SecurityRequirementsPane {

    private SecurityRequirementsPane() {
    }

    /**
     * @param onChanged run after the requirements change, to rebuild this list; invoked on the
     *                  JavaFX thread from inside a control's listener
     */
    public static Node build(SecurityRequirements requirements, SecuritySchemeCatalog catalog,
                             Runnable onChanged) {
        GridPane list = FormFields.grid();
        list.setId("security-requirements");
        list.getColumnConstraints().addAll(
                FormFields.column(HPos.LEFT, Priority.NEVER),
                FormFields.column(HPos.LEFT, Priority.ALWAYS),
                FormFields.column(HPos.RIGHT, Priority.NEVER));

        List<SecurityRequirement> declared = requirements.requirements();
        if (declared.isEmpty()) {
            Label empty = new Label("Nothing required.");
            empty.getStyleClass().add(Styles.TEXT_MUTED);
            list.add(empty, 0, 0, 3, 1);
        } else {
            list.addRow(0, FormFields.columnHeading("Scheme"), FormFields.columnHeading("Scopes"));
            for (int index = 0; index < declared.size(); index++) {
                list.addRow(index + 1, row(declared.get(index), index, requirements, catalog, onChanged));
            }
        }

        ComboBox<String> schemePicker = new ComboBox<>();
        schemePicker.setId("security-requirement-scheme");
        schemePicker.getItems().addAll(catalog.schemeNames());
        schemePicker.setPromptText("scheme");

        Button add = new Button("Add requirement");
        add.setId("add-security-requirement");
        add.getStyleClass().add(Styles.ACCENT);
        add.setOnAction(e -> {
            String schemeName = schemePicker.getValue();
            if (schemeName != null && !schemeName.isBlank()) {
                requirements.add(schemeName);
                onChanged.run();
            }
        });

        HBox addRow = new HBox(8, schemePicker, add);
        addRow.setAlignment(Pos.CENTER_LEFT);
        return new VBox(8, list, addRow);
    }

    private static Node[] row(SecurityRequirement requirement, int index,
                              SecurityRequirements requirements, SecuritySchemeCatalog catalog,
                              Runnable onChanged) {
        Button remove = new Button("Remove");
        remove.setId("remove-security-requirement-" + index);
        remove.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
        remove.setOnAction(e -> {
            requirements.remove(index);
            onChanged.run();
        });

        if (requirement.isCombined()) {
            Label combined = new Label(String.join(" + ", requirement.schemeNames()));
            Label explanation = new Label("all at once — not editable here");
            explanation.getStyleClass().add(Styles.TEXT_MUTED);
            return new Node[]{combined, explanation, remove};
        }

        String schemeName = requirement.schemeNames().isEmpty() ? "" : requirement.schemeNames().get(0);
        Label scheme = new Label(schemeName);
        scheme.setId("security-requirement-" + index + "-scheme");

        List<String> offered = catalog.scopesOf(schemeName);
        if (offered.isEmpty()) {
            Label none = new Label("—");
            none.getStyleClass().add(Styles.TEXT_MUTED);
            return new Node[]{scheme, none, remove};
        }
        return new Node[]{scheme, scopes(requirement, index, schemeName, offered, onChanged), remove};
    }

    private static Node scopes(SecurityRequirement requirement, int index, String schemeName,
                               List<String> offered, Runnable onChanged) {
        FlowPane box = new FlowPane(10, 6);
        box.setId("security-requirement-" + index + "-scopes");
        List<String> required = requirement.getScopes(schemeName);
        for (String scope : offered) {
            // Deliberately no id: a scope name is user data and routinely contains a colon
            // ("read:pets"), which is not valid in a CSS id selector - the checkbox's own text is
            // how it is identified instead.
            CheckBox check = new CheckBox(scope);
            check.setSelected(required.contains(scope));
            check.selectedProperty().addListener((obs, was, now) -> {
                List<String> updated = new ArrayList<>(requirement.getScopes(schemeName));
                if (now) {
                    if (!updated.contains(scope)) {
                        updated.add(scope);
                    }
                } else {
                    updated.remove(scope);
                }
                requirement.setScopes(schemeName, updated);
                onChanged.run();
            });
            box.getChildren().add(check);
        }
        return box;
    }
}
