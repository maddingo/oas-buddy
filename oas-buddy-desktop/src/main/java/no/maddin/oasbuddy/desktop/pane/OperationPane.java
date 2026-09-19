package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.ApiResponse;
import no.maddin.oasbuddy.core.model.Operation;
import no.maddin.oasbuddy.core.model.Parameter;
import no.maddin.oasbuddy.core.model.RequestBody;
import no.maddin.oasbuddy.core.model.Responses;
import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Supplier;

public final class OperationPane {

    private OperationPane() {
    }

    /**
     * @param onRemoveOperation asked to remove this operation; the pane only reports the request
     */
    public static Node build(Operation operation, Supplier<List<String>> schemaNames,
                             Runnable onRemoveOperation) {
        GridPane grid = FormFields.grid();
        int row = 0;
        FormFields.textRow(grid, row++, "Operation ID", operation::getOperationId, operation::setOperationId);
        FormFields.textRow(grid, row++, "Summary", operation::getSummary, operation::setSummary);
        FormFields.textAreaRow(grid, row++, "Description", operation::getDescription, operation::setDescription);
        FormFields.textRow(grid, row,
                "Tags (comma separated)",
                () -> String.join(", ", operation.getTags()),
                value -> operation.setTags(value == null || value.isBlank()
                        ? List.of()
                        : List.of(value.split("\\s*,\\s*"))));

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

        return FormFields.root(
                FormFields.headerWithDelete("Operation", "delete-operation", "Delete operation",
                        onRemoveOperation), grid,
                FormFields.heading("Parameters"), parametersBox, addParameterButton,
                FormFields.heading("Request body"), requestBodyBox,
                FormFields.heading("Responses"), responsesBox, new HBox(8, statusCodeField, addResponseButton));
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
