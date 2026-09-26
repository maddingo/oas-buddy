package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.Parameter;
import no.maddin.oasbuddy.core.model.Parameters;
import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * A {@code parameters} list, the operation's own or a path's. Each row's first picker says where the
 * parameter is defined — inline, or a reference to a component parameter — the same switch an
 * operation response has. Replacing an inline parameter with a reference always asks: unlike a
 * response, an inline parameter always carries at least its name and location, so there is always
 * something to lose. Switching back to inline starts from a copy of the component, loses nothing
 * and never asks.
 */
final class ParametersEditor {

    private ParametersEditor() {
    }

    /**
     * @param styleClass   put on the returned box, so a pane showing two of these (none yet does)
     *                     or a test can tell them apart
     * @param confirmation asked before an inline parameter is replaced by a reference
     */
    static VBox build(Parameters parameters, ComponentCatalog catalog, RemovalConfirmation confirmation,
                      String styleClass) {
        VBox box = new VBox(8);
        box.getStyleClass().add(styleClass);
        fill(parameters, catalog, confirmation, box);
        return box;
    }

    private static void fill(Parameters parameters, ComponentCatalog catalog, RemovalConfirmation confirmation,
                             VBox box) {
        box.getChildren().clear();
        Runnable refresh = () -> fill(parameters, catalog, confirmation, box);

        VBox rows = new VBox(8);
        for (Parameter parameter : parameters.all()) {
            rows.getChildren().add(row(parameters, parameter, catalog, confirmation, refresh));
        }
        if (rows.getChildren().isEmpty()) {
            Label empty = new Label("No parameters.");
            empty.getStyleClass().add(Styles.TEXT_MUTED);
            rows.getChildren().add(empty);
        }

        Button addButton = new Button("Add parameter");
        addButton.getStyleClass().add(Styles.ACCENT);
        addButton.getStyleClass().add("add-parameter");
        addButton.setOnAction(e -> {
            parameters.add("newParam", "query");
            refresh.run();
        });
        HBox actions = new HBox(8, addButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        if (!catalog.parameterNames().isEmpty()) {
            ComboBox<String> componentBox = new ComboBox<>();
            componentBox.getStyleClass().add("reference-parameter");
            componentBox.getItems().addAll(catalog.parameterNames());
            componentBox.setPromptText("Reusable parameter");
            Button addReferenceButton = new Button("Add reference");
            addReferenceButton.getStyleClass().add("add-parameter-reference");
            addReferenceButton.setOnAction(e -> {
                if (componentBox.getValue() != null) {
                    parameters.addReference(componentBox.getValue());
                    refresh.run();
                }
            });
            actions.getChildren().addAll(componentBox, addReferenceButton);
        }

        box.getChildren().addAll(rows, actions);
    }

    private static Node row(Parameters parameters, Parameter parameter, ComponentCatalog catalog,
                            RemovalConfirmation confirmation, Runnable refresh) {
        HBox row = new HBox(8);
        row.getStyleClass().addAll("parameter-row", Styles.BORDERED);
        row.setAlignment(Pos.CENTER_LEFT);

        if (parameter.isReference() && parameter.getReferencedParameterName() == null) {
            // a reference this editor cannot follow (another file, another section): shown, not edited
            Label ref = new Label("$ref: " + parameter.getRef());
            ref.getStyleClass().add(Styles.TEXT_MUTED);
            row.getChildren().add(ref);
        } else {
            row.getChildren().add(sourcePicker(parameters, parameter, catalog, confirmation, refresh));
            if (parameter.isReference()) {
                row.getChildren().add(resolved(catalog.parameter(parameter.getReferencedParameterName())));
            } else {
                row.getChildren().addAll(
                        new Label("Name"), ParameterForm.nameField(parameter),
                        new Label("In"), ParameterForm.inBox(parameter),
                        ParameterForm.requiredBox(parameter),
                        new Label("Type"), ParameterForm.typeField(parameter));
            }
        }

        Button removeButton = new Button("Remove");
        removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
        removeButton.setOnAction(e -> {
            parameters.remove(parameter);
            refresh.run();
        });
        row.getChildren().add(removeButton);
        return row;
    }

    /** What a reference stands for, so the row says more than a component key. */
    private static Label resolved(Parameter component) {
        Label label = new Label(component == null
                ? "(not declared)"
                : component.getName() + " in " + component.getIn());
        label.getStyleClass().addAll(Styles.TEXT_MUTED, "parameter-resolved");
        return label;
    }

    private static ComboBox<SourceChoice> sourcePicker(Parameters parameters, Parameter parameter,
                                                       ComponentCatalog catalog, RemovalConfirmation confirmation,
                                                       Runnable refresh) {
        ComboBox<SourceChoice> picker = new ComboBox<>();
        picker.getStyleClass().add("parameter-source");
        SourceChoice current = parameter.isReference()
                ? new SourceChoice(parameter.getReferencedParameterName())
                : SourceChoice.INLINE;
        picker.getItems().addAll(SourceChoice.choices(catalog.parameterNames(), current));
        picker.setValue(current);

        // Reverting a declined switch would otherwise re-enter this listener.
        boolean[] reverting = {false};
        picker.valueProperty().addListener((obs, was, now) -> {
            if (reverting[0] || now == null || now.equals(was)) {
                return;
            }
            if (now.isInline()) {
                parameters.defineInline(parameter, catalog.parameter(was.name()));
            } else if (!parameter.isReference() && !confirmation.confirm(
                    "Replace the inline parameter \"" + parameter.getName() + "\" with a reference to \""
                            + now.name() + "\"?",
                    "Its name, location and schema are defined only here, and will be discarded.")) {
                reverting[0] = true;
                picker.setValue(was);
                reverting[0] = false;
                return;
            } else {
                parameters.referTo(parameter, now.name());
            }
            refresh.run();
        });
        return picker;
    }
}
