package no.maddin.oasbuddy.desktop;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.DocumentWriter;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import no.maddin.oasbuddy.core.model.Operation;
import no.maddin.oasbuddy.core.model.PathItem;
import no.maddin.oasbuddy.core.validation.OasValidator;
import no.maddin.oasbuddy.core.validation.ValidationMessage;
import no.maddin.oasbuddy.desktop.pane.InfoPane;
import no.maddin.oasbuddy.desktop.pane.OperationPane;
import no.maddin.oasbuddy.desktop.pane.PathItemPane;
import no.maddin.oasbuddy.desktop.pane.PathRemoval;
import no.maddin.oasbuddy.desktop.pane.PathsPane;
import no.maddin.oasbuddy.desktop.pane.SchemaPane;
import no.maddin.oasbuddy.desktop.pane.RemovalConfirmation;
import no.maddin.oasbuddy.desktop.pane.SchemaRemoval;
import no.maddin.oasbuddy.desktop.pane.SchemasPane;
import no.maddin.oasbuddy.desktop.pane.ServersPane;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Styles;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

public class MainApp extends Application {

    private static final Preferences PREFS = Preferences.userNodeForPackage(MainApp.class);
    private static final String THEME_PREF_KEY = "theme";
    private static final String THEME_DARK = "dark";
    private static final String THEME_LIGHT = "light";

    private final OasValidator validator = new OasValidator();

    /** Overridden by tests, which cannot answer a modal dialog. */
    private RemovalConfirmation confirmation = RemovalConfirmation.dialog();

    private Stage stage;
    private OasDocument document;
    private Path currentFile;

    private TreeView<OutlineNode> outlineView;
    private BorderPane centerHolder;
    private ListView<String> validationList;

    @Override
    public void start(Stage primaryStage) {
        boolean darkTheme = THEME_DARK.equals(PREFS.get(THEME_PREF_KEY, THEME_LIGHT));
        setTheme(darkTheme);

        this.stage = primaryStage;
        this.document = OasDocument.newDocument(DocumentFormat.YAML);

        BorderPane root = new BorderPane();
        root.setTop(buildMenuBar(darkTheme));

        outlineView = new TreeView<>();
        outlineView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null && newItem.getValue() != null && newItem.getValue().paneSupplier() != null) {
                showPane(newItem.getValue().paneSupplier().get());
            }
        });
        outlineView.setPrefWidth(240);
        outlineView.getStyleClass().add(Styles.DENSE);

        centerHolder = new BorderPane();
        centerHolder.getStyleClass().add(Styles.BG_SUBTLE);

        Label validationHeading = new Label("Validation");
        validationHeading.getStyleClass().add(Styles.TITLE_4);
        Button validateButton = new Button("Validate");
        validateButton.getStyleClass().add(Styles.ACCENT);
        validateButton.setOnAction(e -> runValidation());
        HBox validationHeader = new HBox(8, validationHeading, spacer(), validateButton);
        validationHeader.setAlignment(Pos.CENTER_LEFT);

        validationList = new ListView<>();
        validationList.setPrefHeight(120);

        VBox validationBox = new VBox(6, validationHeader, validationList);
        validationBox.setPadding(new Insets(10, 12, 12, 12));

        root.setLeft(outlineView);
        root.setCenter(centerHolder);
        root.setBottom(new VBox(new Separator(), validationBox));

        refreshOutline();

        stage.setTitle("OAS Buddy");
        stage.setScene(new Scene(root, 1100, 750));
        stage.show();
    }

    private static Node spacer() {
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private MenuBar buildMenuBar(boolean darkTheme) {
        MenuItem open = new MenuItem("Open...");
        open.setOnAction(e -> openFile());

        MenuItem save = new MenuItem("Save");
        save.setOnAction(e -> save());

        MenuItem saveAs = new MenuItem("Save As...");
        saveAs.setOnAction(e -> saveAs());

        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> Platform.exit());

        Menu fileMenu = new Menu("File", null, open, save, saveAs, exit);

        ToggleGroup themeGroup = new ToggleGroup();
        RadioMenuItem lightThemeItem = new RadioMenuItem("Light");
        lightThemeItem.setToggleGroup(themeGroup);
        lightThemeItem.setSelected(!darkTheme);
        lightThemeItem.setOnAction(e -> setTheme(false));

        RadioMenuItem darkThemeItem = new RadioMenuItem("Dark");
        darkThemeItem.setToggleGroup(themeGroup);
        darkThemeItem.setSelected(darkTheme);
        darkThemeItem.setOnAction(e -> setTheme(true));

        Menu viewMenu = new Menu("View", null, lightThemeItem, darkThemeItem);

        return new MenuBar(fileMenu, viewMenu);
    }

    private void setTheme(boolean dark) {
        Application.setUserAgentStylesheet(dark
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());
        PREFS.put(THEME_PREF_KEY, dark ? THEME_DARK : THEME_LIGHT);
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

    /** Package-private so tests can rebuild the outline after seeding the document. */
    void refreshOutline() {
        TreeItem<OutlineNode> root = new TreeItem<>(new OutlineNode(titleOrDefault(), null));
        root.setExpanded(true);

        root.getChildren().add(new TreeItem<>(
                new OutlineNode("Info", () -> InfoPane.build(document.getInfo()))));
        root.getChildren().add(new TreeItem<>(
                new OutlineNode("Servers", () -> ServersPane.build(document))));

        TreeItem<OutlineNode> pathsItem = new TreeItem<>(new OutlineNode(
                "Paths", () -> PathsPane.build(document, this::refreshOutline, this::removePath)));
        pathsItem.setExpanded(true);
        for (String path : document.getPaths().pathNames()) {
            PathItem pathItem = document.getPaths().getPathItem(path);
            TreeItem<OutlineNode> pathNode = new TreeItem<>(new OutlineNode(path,
                    () -> PathItemPane.build(document, path, this::refreshOutline,
                            () -> removePath(path), method -> removeOperation(path, method))));
            for (var entry : pathItem.getOperations().entrySet()) {
                HttpMethod method = entry.getKey();
                Operation operation = entry.getValue();
                Supplier<List<String>> schemaNames = () -> document.getComponents().getSchemas().names();
                pathNode.getChildren().add(new TreeItem<>(new OutlineNode(method.name(),
                        () -> OperationPane.build(operation, schemaNames,
                                () -> removeOperation(path, method)))));
            }
            pathsItem.getChildren().add(pathNode);
        }
        root.getChildren().add(pathsItem);

        TreeItem<OutlineNode> schemasItem = new TreeItem<>(new OutlineNode(
                "Schemas", () -> SchemasPane.build(document, this::refreshOutline, this::removeSchema)));
        schemasItem.setExpanded(true);
        for (String name : document.getComponents().getSchemas().names()) {
            schemasItem.getChildren().add(new TreeItem<>(
                    new OutlineNode(name, () -> SchemaPane.build(document, name, this::removeSchema))));
        }
        root.getChildren().add(schemasItem);

        outlineView.setRoot(root);
        outlineView.setShowRoot(true);
    }

    private void removeSchema(String schemaName) {
        SchemaRemoval.remove(document, schemaName, confirmation,
                () -> refreshAndSelect("Schemas"));
    }

    private void removePath(String path) {
        PathRemoval.removePath(document, path, confirmation,
                () -> refreshAndSelect("Paths"));
    }

    private void removeOperation(String path, HttpMethod method) {
        PathRemoval.removeOperation(document, path, method, confirmation,
                () -> refreshAndSelect("Paths", path));
    }

    /**
     * Rebuilds the outline after a removal and selects what is left, so the editor is never left
     * showing something that no longer exists.
     */
    private void refreshAndSelect(String... labels) {
        refreshOutline();
        TreeItem<OutlineNode> item = outlineView.getRoot();
        for (String label : labels) {
            item = item.getChildren().stream()
                    .filter(child -> label.equals(child.getValue().label()))
                    .findFirst()
                    .orElse(null);
            if (item == null) {
                return;
            }
        }
        outlineView.getSelectionModel().select(item);
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

    void setConfirmation(RemovalConfirmation confirmation) {
        this.confirmation = confirmation;
    }

    private record OutlineNode(String label, Supplier<Node> paneSupplier) {
        @Override
        public String toString() {
            return label;
        }
    }
}
