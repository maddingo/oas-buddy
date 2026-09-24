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
import no.maddin.oasbuddy.core.validation.ValidationSeverity;
import no.maddin.oasbuddy.desktop.pane.AboutDialog;
import no.maddin.oasbuddy.desktop.pane.InfoPane;
import no.maddin.oasbuddy.desktop.pane.OperationPane;
import no.maddin.oasbuddy.desktop.pane.PathItemPane;
import no.maddin.oasbuddy.desktop.pane.PathRemoval;
import no.maddin.oasbuddy.desktop.pane.PathsPane;
import no.maddin.oasbuddy.desktop.pane.SchemaPane;
import no.maddin.oasbuddy.desktop.pane.RemovalConfirmation;
import no.maddin.oasbuddy.desktop.pane.SchemaRemoval;
import no.maddin.oasbuddy.desktop.pane.SchemasPane;
import no.maddin.oasbuddy.desktop.pane.SecurityPane;
import no.maddin.oasbuddy.desktop.pane.SecuritySchemeCatalog;
import no.maddin.oasbuddy.desktop.pane.SecuritySchemePane;
import no.maddin.oasbuddy.desktop.pane.SecuritySchemeRemoval;
import no.maddin.oasbuddy.desktop.pane.SecuritySchemesPane;
import no.maddin.oasbuddy.desktop.pane.ServersPane;
import no.maddin.oasbuddy.desktop.pane.TagCatalog;
import no.maddin.oasbuddy.desktop.pane.TagRemoval;
import no.maddin.oasbuddy.desktop.pane.TagsPane;
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

    /** Overridden by tests, which cannot dismiss a modal dialog. */
    private Runnable aboutAction = this::showAbout;

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
        stage.getIcons().addAll(AppIcon.icons(16, 32, 48, 64, 128, 256));
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

        MenuItem about = new MenuItem("About OAS Buddy");
        about.setOnAction(e -> aboutAction.run());

        Menu helpMenu = new Menu("Help", null, about);

        return new MenuBar(fileMenu, viewMenu, helpMenu);
    }

    private void showAbout() {
        AboutDialog.show(stage, AppInfo.load(), this::browse);
    }

    /**
     * Hands a url to the desktop's own browser.
     *
     * <p>Package-private so the tests can exercise the case that actually happens: {@code
     * HostServices} only exists in an application JavaFX launched itself, and a desktop with no
     * registered handler fails at {@code showDocument}. A dead About link is not worth a stack
     * trace in the user's face, so either way this logs and returns.
     */
    void browse(String url) {
        try {
            getHostServices().showDocument(url);
        } catch (RuntimeException e) {
            System.getLogger(MainApp.class.getName())
                    .log(System.Logger.Level.WARNING, "could not open " + url + " in a browser", e);
        }
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

    /**
     * Both severities are shown, prefixed so they stay tellable apart at a glance; the fallback
     * text only appears when there are neither errors nor warnings, so it stays accurate now that a
     * warning-only document also gets messages here.
     */
    private void runValidation() {
        var result = validator.validate(document);
        List<String> messages = result.messages().stream().map(MainApp::describe).toList();
        validationList.getItems().setAll(messages.isEmpty() ? List.of("No validation errors.") : messages);
    }

    private static String describe(ValidationMessage message) {
        String prefix = message.severity() == ValidationSeverity.WARNING ? "Warning: " : "Error: ";
        return prefix + message.message();
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
        root.getChildren().add(new TreeItem<>(new OutlineNode(
                "Tags", () -> TagsPane.build(document, this::refreshOutline, this::removeTag))));

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
                                TagCatalog.of(document), SecuritySchemeCatalog.of(document),
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

        TreeItem<OutlineNode> securitySchemesItem = new TreeItem<>(new OutlineNode(
                "Security Schemes",
                () -> SecuritySchemesPane.build(document, this::refreshOutline, this::removeSecurityScheme)));
        securitySchemesItem.setExpanded(true);
        for (String name : document.getComponents().getSecuritySchemes().names()) {
            securitySchemesItem.getChildren().add(new TreeItem<>(new OutlineNode(name,
                    () -> SecuritySchemePane.build(document, name, confirmation,
                            this::removeSecurityScheme))));
        }
        root.getChildren().add(securitySchemesItem);

        root.getChildren().add(new TreeItem<>(new OutlineNode(
                "Security", () -> SecurityPane.build(document, this::refreshOutline))));

        outlineView.setRoot(root);
        outlineView.setShowRoot(true);
    }

    private void removeSchema(String schemaName) {
        SchemaRemoval.remove(document, schemaName, confirmation,
                () -> refreshAndSelect("Schemas"));
    }

    private void removeTag(String tagName) {
        TagRemoval.remove(document, tagName, confirmation,
                () -> refreshAndSelect("Tags"));
    }

    private void removeSecurityScheme(String schemeName) {
        SecuritySchemeRemoval.remove(document, schemeName, confirmation,
                () -> refreshAndSelect("Security Schemes"));
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

    void setAboutAction(Runnable aboutAction) {
        this.aboutAction = aboutAction;
    }

    private record OutlineNode(String label, Supplier<Node> paneSupplier) {
        @Override
        public String toString() {
            return label;
        }
    }
}
