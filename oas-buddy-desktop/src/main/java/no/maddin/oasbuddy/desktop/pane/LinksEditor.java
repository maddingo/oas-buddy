package no.maddin.oasbuddy.desktop.pane;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import no.maddin.oasbuddy.core.model.Link;
import no.maddin.oasbuddy.core.model.ResponseLinks;

/**
 * The links of a response, used by an operation's response cards and by the component response
 * editor. Each link is inline or a reference to {@code components.links}, switched with the
 * same picker and the same "ask only when there is something to lose" rule as responses.
 *
 * <p>Nothing carries an id: link names are user data and several responses are on screen at once.
 */
final class LinksEditor {

    static final String ROW_CLASS = "link-row";
    static final String NAME_CLASS = "link-name";
    static final String SOURCE_CLASS = "link-source";
    static final String NEW_NAME_CLASS = "new-link-name";
    static final String ADD_CLASS = "add-link";
    static final String REFERENCE_CLASS = "reference-link";
    static final String ADD_REFERENCE_CLASS = "add-link-reference";

    private LinksEditor() {
    }

    static VBox build(ResponseLinks links, ComponentCatalog catalog, RemovalConfirmation confirmation) {
        VBox box = new VBox(6);
        refresh(links, catalog, confirmation, box);
        return box;
    }

    private static void refresh(ResponseLinks links, ComponentCatalog catalog, RemovalConfirmation confirmation,
                                VBox box) {
        box.getChildren().clear();
        Runnable refresh = () -> refresh(links, catalog, confirmation, box);

        for (String name : links.names()) {
            box.getChildren().add(row(links, name, catalog, confirmation, refresh));
        }

        TextField nameField = new TextField();
        nameField.getStyleClass().add(NEW_NAME_CLASS);
        nameField.setPromptText("link name");
        Button addButton = new Button("Add link");
        addButton.getStyleClass().addAll(Styles.SMALL, ADD_CLASS);
        Runnable add = () -> {
            String name = nameField.getText();
            if (name != null && !name.isBlank() && !links.names().contains(name.strip())) {
                links.add(name.strip());
                refresh.run();
            }
        };
        addButton.setOnAction(e -> add.run());
        nameField.setOnAction(e -> add.run());
        HBox addRow = new HBox(8, nameField, addButton);
        addRow.setAlignment(Pos.CENTER_LEFT);

        if (!catalog.linkNames().isEmpty()) {
            ComboBox<String> components = new ComboBox<>();
            components.getStyleClass().add(REFERENCE_CLASS);
            components.setPromptText("Reusable link");
            components.getItems().addAll(catalog.linkNames());
            Button referButton = new Button("Add reference");
            referButton.getStyleClass().addAll(Styles.SMALL, ADD_REFERENCE_CLASS);
            referButton.setOnAction(e -> {
                String component = components.getValue();
                // named after the component, since that is what a link is usually called
                if (component != null && !links.names().contains(component)) {
                    links.referTo(component, component);
                    refresh.run();
                }
            });
            addRow.getChildren().addAll(components, referButton);
        }
        box.getChildren().add(addRow);
    }

    private static HBox row(ResponseLinks links, String name, ComponentCatalog catalog,
                            RemovalConfirmation confirmation, Runnable refresh) {
        Link link = links.get(name);

        TextField nameField = FormFields.renameField(name, candidate ->
                links.names().contains(candidate)
                        ? "A link named \"" + candidate + "\" already exists."
                        : links.rename(name, candidate) ? null : "");
        nameField.getStyleClass().add(NAME_CLASS);
        nameField.setPrefColumnCount(12);
        // the row is rebuilt after a rename so its buttons act on the new key
        nameField.focusedProperty().addListener((obs, was, is) -> {
            if (!is && !links.names().contains(name)) {
                refresh.run();
            }
        });

        HBox row = new HBox(8, nameField);
        row.getStyleClass().add(ROW_CLASS);
        row.setAlignment(Pos.TOP_LEFT);

        if (link == null) {
            Label note = FormFields.notAnObject();
            row.getChildren().add(note);
        } else if (link.isReference() && link.getReferencedLinkName() == null) {
            // a reference this editor cannot follow: shown, not edited
            Label ref = new Label("$ref: " + link.getRef());
            ref.getStyleClass().add(Styles.TEXT_MUTED);
            row.getChildren().add(ref);
        } else {
            row.getChildren().add(sourcePicker(links, name, link, catalog, confirmation, refresh));
            if (!link.isReference()) {
                VBox form = LinkForm.build(link, catalog);
                HBox.setHgrow(form, Priority.ALWAYS);
                row.getChildren().add(form);
            }
        }

        Button removeButton = new Button("Remove");
        removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED, Styles.SMALL);
        removeButton.setOnAction(e -> {
            links.remove(name);
            refresh.run();
        });
        row.getChildren().add(removeButton);
        return row;
    }

    private static ComboBox<SourceChoice> sourcePicker(ResponseLinks links, String name, Link link,
                                                       ComponentCatalog catalog, RemovalConfirmation confirmation,
                                                       Runnable refresh) {
        ComboBox<SourceChoice> picker = new ComboBox<>();
        picker.getStyleClass().add(SOURCE_CLASS);
        SourceChoice current = link.isReference()
                ? new SourceChoice(link.getReferencedLinkName())
                : SourceChoice.INLINE;
        picker.getItems().addAll(SourceChoice.choices(catalog.linkNames(), current));
        picker.setValue(current);

        // Reverting a declined switch would otherwise re-enter this listener.
        boolean[] reverting = {false};
        picker.valueProperty().addListener((obs, was, now) -> {
            if (reverting[0] || now == null || now.equals(was)) {
                return;
            }
            if (now.isInline()) {
                links.defineInline(name, catalog.link(was.name()));
            } else if (!link.isReference() && !link.isEmpty() && !confirmation.confirm(
                    "Replace the inline \"" + name + "\" link with a reference to \"" + now.name() + "\"?",
                    "Its target, parameters and description are defined only here, and will be discarded.")) {
                reverting[0] = true;
                picker.setValue(was);
                reverting[0] = false;
                return;
            } else {
                links.referTo(name, now.name());
            }
            refresh.run();
        });
        return picker;
    }
}
