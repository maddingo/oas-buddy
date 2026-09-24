package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.ApiResponse;
import no.maddin.oasbuddy.core.model.Operation;
import no.maddin.oasbuddy.core.model.Parameter;
import no.maddin.oasbuddy.core.model.RequestBody;
import no.maddin.oasbuddy.core.model.Responses;
import no.maddin.oasbuddy.core.model.SecurityRequirements;
import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class OperationPane {

    private OperationPane() {
    }

    /**
     * @param tagCatalog        the tags this operation's picker may offer, and where a tag typed
     *                          inline is declared
     * @param catalog           the schemes a security override may point at
     * @param onRemoveOperation asked to remove this operation; the pane only reports the request
     */
    public static Node build(Operation operation, Supplier<List<String>> schemaNames,
                             TagCatalog tagCatalog, SecuritySchemeCatalog catalog, Runnable onRemoveOperation) {
        GridPane grid = FormFields.grid();
        int row = 0;
        FormFields.textRow(grid, row++, "Operation ID", operation::getOperationId, operation::setOperationId);
        FormFields.textRow(grid, row++, "Summary", operation::getSummary, operation::setSummary);
        FormFields.textAreaRow(grid, row, "Description", operation::getDescription, operation::setDescription);

        VBox tagsBox = new VBox(8);
        buildTags(operation, tagsBox, tagCatalog);

        VBox parametersBox = new VBox(8);
        refreshParameters(operation, parametersBox);
        Button addParameterButton = new Button("Add parameter");
        addParameterButton.getStyleClass().add(Styles.ACCENT);
        addParameterButton.setOnAction(e -> {
            operation.addParameter("newParam", "query");
            refreshParameters(operation, parametersBox);
        });

        VBox requestBodyBox = new VBox(8);
        buildRequestBody(operation, requestBodyBox, schemaNames);

        VBox responsesBox = new VBox(8);
        refreshResponses(operation, responsesBox, schemaNames);
        TextField statusCodeField = new TextField();
        statusCodeField.setPromptText("status code, e.g. 200");
        Button addResponseButton = new Button("Add response");
        addResponseButton.getStyleClass().add(Styles.ACCENT);
        addResponseButton.setOnAction(e -> {
            String code = statusCodeField.getText();
            if (code != null && !code.isBlank()) {
                operation.getResponses().addResponse(code.strip());
                statusCodeField.clear();
                refreshResponses(operation, responsesBox, schemaNames);
            }
        });

        VBox securityBox = new VBox(8);
        buildSecurity(operation, securityBox, catalog);

        return FormFields.root(
                FormFields.headerWithDelete("Operation", "delete-operation", "Delete operation",
                        onRemoveOperation), grid,
                FormFields.heading("Tags"), tagsBox,
                FormFields.heading("Parameters"), parametersBox, addParameterButton,
                FormFields.heading("Request body"), requestBodyBox,
                FormFields.heading("Responses"), responsesBox, new HBox(8, statusCodeField, addResponseButton),
                FormFields.heading("Security"), securityBox);
    }

    /**
     * The three states the spec distinguishes: no {@code security} key (inherit the document
     * default), an empty array (explicitly public) and a non-empty array (override).
     *
     * <p>Which radio is selected is read from the document on build, but held as the pane's own
     * state afterwards: choosing "Require" declares an empty array, which on its own still reads
     * back as "public", and the radio must not jump away while the user is about to add the first
     * requirement.
     */
    private static void buildSecurity(Operation operation, VBox box, SecuritySchemeCatalog catalog) {
        SecurityRequirements security = operation.getSecurity();
        String mode = !security.isDeclared() ? "inherit"
                : security.requirements().isEmpty() ? "public" : "require";
        buildSecurity(operation, box, catalog, mode);
    }

    private static void buildSecurity(Operation operation, VBox box, SecuritySchemeCatalog catalog,
                                      String mode) {
        box.getChildren().clear();
        SecurityRequirements security = operation.getSecurity();

        ToggleGroup group = new ToggleGroup();
        HBox choices = new HBox(12,
                modeRadio(group, "inherit", "Inherit document default", mode,
                        () -> security.undeclare(), operation, box, catalog),
                modeRadio(group, "public", "Public (no security)", mode,
                        security::declarePublic, operation, box, catalog),
                modeRadio(group, "require", "Require", mode,
                        () -> { }, operation, box, catalog));
        choices.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(choices);

        if ("require".equals(mode)) {
            // "Require" with nothing listed is an empty array, which is what public looks like; the
            // array is declared here so adding the first requirement has somewhere to go.
            if (!security.isDeclared()) {
                security.declarePublic();
            }
            box.getChildren().add(SecurityRequirementsPane.build(security, catalog,
                    () -> buildSecurity(operation, box, catalog, "require")));
        }
    }

    private static RadioButton modeRadio(ToggleGroup group, String mode, String label, String current,
                                         Runnable apply, Operation operation, VBox box,
                                         SecuritySchemeCatalog catalog) {
        RadioButton radio = new RadioButton(label);
        radio.setId("operation-security-" + mode);
        radio.setToggleGroup(group);
        radio.setSelected(mode.equals(current));
        radio.selectedProperty().addListener((obs, was, now) -> {
            if (now) {
                apply.run();
                buildSecurity(operation, box, catalog, mode);
            }
        });
        return radio;
    }

    /**
     * The tag picker: one checkbox per tag the document defines, plus one per tag the operation
     * already uses that is not defined — shown checked and marked undefined rather than hidden, so
     * the form never disagrees with a loaded file. A tag typed into the field at the bottom is
     * declared at the root and added to the operation in the same action, so tagging stays one step.
     *
     * <p>Toggling rebuilds this box rather than patching it in place: unlike {@link #buildSecurity},
     * where the field being edited would fight a rebuild, a checkbox has no caret to lose, and
     * rebuilding is what makes a newly-undefined checkbox disappear again once it is unticked.
     */
    private static void buildTags(Operation operation, VBox box, TagCatalog tagCatalog) {
        box.getChildren().clear();

        List<String> defined = tagCatalog.tagNames();
        List<String> current = operation.getTags();

        VBox checkBoxes = new VBox(4);
        checkBoxes.setId("operation-tags");
        for (String name : defined) {
            checkBoxes.getChildren().add(
                    tagCheckBox(operation, box, tagCatalog, name, current.contains(name), false));
        }
        for (String name : current) {
            if (!defined.contains(name)) {
                checkBoxes.getChildren().add(tagCheckBox(operation, box, tagCatalog, name, true, true));
            }
        }
        if (checkBoxes.getChildren().isEmpty()) {
            Label empty = new Label("No tags defined yet.");
            empty.getStyleClass().add(Styles.TEXT_MUTED);
            checkBoxes.getChildren().add(empty);
        }

        TextField newTagField = new TextField();
        newTagField.setId("operation-new-tag-name");
        newTagField.setPromptText("New tag");
        Runnable addNewTag = () -> {
            String name = newTagField.getText();
            if (name == null || name.isBlank()) {
                return;
            }
            String trimmed = name.strip();
            tagCatalog.ensureDeclared(trimmed);
            List<String> tags = new ArrayList<>(operation.getTags());
            if (!tags.contains(trimmed)) {
                tags.add(trimmed);
                operation.setTags(tags);
            }
            newTagField.clear();
            buildTags(operation, box, tagCatalog);
        };
        newTagField.setOnAction(e -> addNewTag.run());
        Button addTagButton = new Button("Add tag");
        addTagButton.setId("operation-add-tag");
        addTagButton.getStyleClass().add(Styles.ACCENT);
        addTagButton.setOnAction(e -> addNewTag.run());

        box.getChildren().addAll(checkBoxes, new HBox(8, newTagField, addTagButton));
    }

    /**
     * No id: a tag name routinely appears nowhere else to key off, but the same gotcha that rules
     * out an id built from an OAuth scope name applies here too, so the checkbox's own label is
     * what tests look up by.
     */
    private static CheckBox tagCheckBox(Operation operation, VBox box, TagCatalog tagCatalog,
                                        String name, boolean selected, boolean undefined) {
        CheckBox checkBox = new CheckBox(undefined ? name + " (undefined)" : name);
        checkBox.setSelected(selected);
        if (undefined) {
            checkBox.getStyleClass().add(Styles.WARNING);
        }
        checkBox.selectedProperty().addListener((obs, was, now) -> {
            List<String> tags = new ArrayList<>(operation.getTags());
            if (now) {
                if (!tags.contains(name)) {
                    tags.add(name);
                }
            } else {
                tags.remove(name);
            }
            operation.setTags(tags);
            buildTags(operation, box, tagCatalog);
        });
        return checkBox;
    }

    private static void refreshParameters(Operation operation, VBox box) {
        box.getChildren().clear();
        for (Parameter parameter : operation.getParameters()) {
            TextField nameField = new TextField(nullToEmpty(parameter.getName()));
            nameField.textProperty().addListener((obs, oldVal, newVal) -> parameter.setName(newVal));

            ComboBox<String> inBox = new ComboBox<>();
            inBox.getItems().addAll("query", "path", "header", "cookie");
            inBox.setValue(parameter.getIn());
            inBox.valueProperty().addListener((obs, oldVal, newVal) -> parameter.setIn(newVal));

            CheckBox requiredBox = new CheckBox("Required");
            requiredBox.setSelected(Boolean.TRUE.equals(parameter.isRequired()));
            requiredBox.selectedProperty().addListener((obs, oldVal, newVal) -> parameter.setRequired(newVal));

            TextField typeField = new TextField(nullToEmpty(parameter.getSchema().getType()));
            typeField.setPromptText("type");
            typeField.textProperty().addListener((obs, oldVal, newVal) -> parameter.getSchema().setType(newVal));

            HBox row = new HBox(8,
                    new Label("Name"), nameField,
                    new Label("In"), inBox,
                    requiredBox,
                    new Label("Type"), typeField);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add(Styles.BORDERED);
            box.getChildren().add(row);
        }
    }

    private static void buildRequestBody(Operation operation, VBox box, Supplier<List<String>> schemaNames) {
        RequestBody existing = operation.getRequestBody();
        if (existing == null) {
            Button addButton = new Button("Add request body");
            addButton.getStyleClass().add(Styles.ACCENT);
            addButton.setOnAction(e -> {
                operation.addRequestBody();
                buildRequestBody(operation, box, schemaNames);
            });
            box.getChildren().setAll(addButton);
            return;
        }

        CheckBox requiredBox = new CheckBox("Required");
        requiredBox.setSelected(Boolean.TRUE.equals(existing.isRequired()));
        requiredBox.selectedProperty().addListener((obs, oldVal, newVal) -> existing.setRequired(newVal));

        TextField descriptionField = new TextField(nullToEmpty(existing.getDescription()));
        descriptionField.textProperty().addListener((obs, oldVal, newVal) -> existing.setDescription(newVal));

        ComboBox<String> schemaBox = new ComboBox<>();
        schemaBox.getItems().addAll(schemaNames.get());
        schemaBox.setValue(refToSchemaName(existing.getSchema("application/json").getRef()));
        schemaBox.valueProperty().addListener((obs, oldVal, newVal) ->
                existing.getSchema("application/json").setRef(newVal == null ? null : "#/components/schemas/" + newVal));

        HBox row = new HBox(8,
                requiredBox,
                new Label("Description"), descriptionField,
                new Label("Schema (application/json)"), schemaBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(Styles.BORDERED);
        box.getChildren().setAll(row);
    }

    private static void refreshResponses(Operation operation, VBox box, Supplier<List<String>> schemaNames) {
        box.getChildren().clear();
        Responses responses = operation.getResponses();
        for (String statusCode : responses.statusCodes()) {
            ApiResponse response = responses.getResponse(statusCode);

            TextField descriptionField = new TextField(nullToEmpty(response.getDescription()));
            descriptionField.textProperty().addListener((obs, oldVal, newVal) -> response.setDescription(newVal));

            ComboBox<String> schemaBox = new ComboBox<>();
            schemaBox.getItems().addAll(schemaNames.get());
            schemaBox.setValue(refToSchemaName(response.getSchema("application/json").getRef()));
            schemaBox.valueProperty().addListener((obs, oldVal, newVal) ->
                    response.getSchema("application/json").setRef(newVal == null ? null : "#/components/schemas/" + newVal));

            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
            removeButton.setOnAction(e -> {
                responses.removeResponse(statusCode);
                refreshResponses(operation, box, schemaNames);
            });

            HBox row = new HBox(8,
                    new Label(statusCode),
                    new Label("Description"), descriptionField,
                    new Label("Schema"), schemaBox,
                    removeButton);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add(Styles.BORDERED);
            box.getChildren().add(row);
        }
    }

    private static String refToSchemaName(String ref) {
        if (ref == null) {
            return null;
        }
        int idx = ref.lastIndexOf('/');
        return idx >= 0 ? ref.substring(idx + 1) : ref;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
