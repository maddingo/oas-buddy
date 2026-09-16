package no.maddin.oasbuddy.desktop;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.DocumentWriter;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import no.maddin.oasbuddy.core.model.Operation;
import no.maddin.oasbuddy.core.model.PathItem;
import no.maddin.oasbuddy.core.model.Schema;
import no.maddin.oasbuddy.core.validation.OasValidator;
import no.maddin.oasbuddy.core.validation.ValidationMessage;
import no.maddin.oasbuddy.desktop.pane.InfoPane;
import no.maddin.oasbuddy.desktop.pane.OperationPane;
import no.maddin.oasbuddy.desktop.pane.PathItemPane;
import no.maddin.oasbuddy.desktop.pane.PathsPane;
import no.maddin.oasbuddy.desktop.pane.SchemaPane;
import no.maddin.oasbuddy.desktop.pane.SchemasPane;
import no.maddin.oasbuddy.desktop.pane.ServersPane;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

public class MainApp extends Application {

    private final OasValidator validator = new OasValidator();

    private Stage stage;
    private OasDocument document;
    private Path currentFile;

    private TreeView<OutlineNode> outlineView;
    private BorderPane centerHolder;
    private ListView<String> validationList;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        this.document = OasDocument.newDocument(DocumentFormat.YAML);

        BorderPane root = new BorderPane();
        root.setTop(buildMenuBar());

        outlineView = new TreeView<>();
        outlineView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null && newItem.getValue() != null && newItem.getValue().paneSupplier() != null) {
                showPane(newItem.getValue().paneSupplier().get());
            }
        });
        outlineView.setPrefWidth(240);

        centerHolder = new BorderPane();

        validationList = new ListView<>();
        validationList.setPrefHeight(120);
        Button validateButton = new Button("Validate");
        validateButton.setOnAction(e -> runValidation());
        VBox validationBox = new VBox(4, new HBox(8, new Label("Validation"), validateButton), validationList);

        root.setLeft(outlineView);
        root.setCenter(centerHolder);
        root.setBottom(validationBox);

        refreshOutline();

        stage.setTitle("OAS Buddy");
        stage.setScene(new Scene(root, 1000, 700));
        stage.show();
    }

    private MenuBar buildMenuBar() {
        MenuItem open = new MenuItem("Open...");
        open.setOnAction(e -> openFile());

        MenuItem save = new MenuItem("Save");
        save.setOnAction(e -> save());

        MenuItem saveAs = new MenuItem("Save As...");
        saveAs.setOnAction(e -> saveAs());

        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> Platform.exit());

        Menu fileMenu = new Menu("File", null, open, save, saveAs, exit);
        return new MenuBar(fileMenu);
    }

    private void openFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open OpenAPI document");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("OpenAPI files", "*.yaml", "*.yml", "*.json"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        var file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            document = DocumentReader.read(file.toPath());
            currentFile = file.toPath();
            refreshOutline();
            validationList.getItems().clear();
        } catch (Exception e) {
            showError("Could not open file", e.getMessage());
        }
    }

    private void save() {
        if (currentFile == null) {
            saveAs();
            return;
        }
        writeToFile(currentFile);
    }

    private void saveAs() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save OpenAPI document");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("OpenAPI files", "*.yaml", "*.yml", "*.json"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        var file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        currentFile = file.toPath();
        writeToFile(currentFile);
    }

    private void writeToFile(Path file) {
        try {
            DocumentWriter.write(document, file);
            runValidation();
        } catch (IOException e) {
            showError("Could not save file", e.getMessage());
        }
    }

    private void runValidation() {
        var result = validator.validate(document);
        if (result.isValid()) {
            validationList.getItems().setAll(List.of("No validation errors."));
        } else {
            validationList.getItems().setAll(result.messages().stream().map(ValidationMessage::message).toList());
        }
    }

    private void showPane(Node pane) {
        centerHolder.setCenter(new ScrollPane(pane));
    }

    private void refreshOutline() {
        TreeItem<OutlineNode> root = new TreeItem<>(new OutlineNode(titleOrDefault(), null));
        root.setExpanded(true);

        root.getChildren().add(new TreeItem<>(
                new OutlineNode("Info", () -> InfoPane.build(document.getInfo()))));
        root.getChildren().add(new TreeItem<>(
                new OutlineNode("Servers", () -> ServersPane.build(document))));

        TreeItem<OutlineNode> pathsItem = new TreeItem<>(
                new OutlineNode("Paths", () -> PathsPane.build(document.getPaths(), this::refreshOutline)));
        pathsItem.setExpanded(true);
        for (String path : document.getPaths().pathNames()) {
            PathItem pathItem = document.getPaths().getPathItem(path);
            TreeItem<OutlineNode> pathNode = new TreeItem<>(
                    new OutlineNode(path, () -> PathItemPane.build(pathItem, this::refreshOutline)));
            for (var entry : pathItem.getOperations().entrySet()) {
                HttpMethod method = entry.getKey();
                Operation operation = entry.getValue();
                Supplier<List<String>> schemaNames = () -> document.getComponents().getSchemas().names();
                pathNode.getChildren().add(new TreeItem<>(
                        new OutlineNode(method.name(), () -> OperationPane.build(operation, schemaNames))));
            }
            pathsItem.getChildren().add(pathNode);
        }
        root.getChildren().add(pathsItem);

        TreeItem<OutlineNode> schemasItem = new TreeItem<>(new OutlineNode(
                "Schemas", () -> SchemasPane.build(document.getComponents().getSchemas(), this::refreshOutline)));
        schemasItem.setExpanded(true);
        for (String name : document.getComponents().getSchemas().names()) {
            Schema schema = document.getComponents().getSchemas().getSchema(name);
            schemasItem.getChildren().add(new TreeItem<>(
                    new OutlineNode(name, () -> SchemaPane.build(schema))));
        }
        root.getChildren().add(schemasItem);

        outlineView.setRoot(root);
        outlineView.setShowRoot(true);
    }

    private String titleOrDefault() {
        String title = document.getInfo().getTitle();
        return title == null || title.isBlank() ? "OAS Buddy" : title;
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("OAS Buddy");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    OasDocument getDocument() {
        return document;
    }

    private record OutlineNode(String label, Supplier<Node> paneSupplier) {
        @Override
        public String toString() {
            return label;
        }
    }
}
