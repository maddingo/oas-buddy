package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import no.maddin.oasbuddy.core.model.Operation;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The operation tag picker: defined tags to choose from, an undefined tag shown rather than hidden,
 * and an inline "new tag" field that declares at the root and applies to the operation together.
 */
class OperationPaneTagsTest extends ApplicationTest {

    private OasDocument document;
    private StackPane holder;

    @Override
    public void start(Stage stage) {
        document = DocumentReader.read("""
                openapi: 3.0.3
                info:
                  title: Tagged
                  version: 1.0.0
                tags:
                  - name: pets
                  - name: store
                paths:
                  /pets:
                    get:
                      operationId: listPets
                      tags: [pets, ghost]
                      responses:
                        '200':
                          description: ok
                """, DocumentFormat.YAML);
        holder = new StackPane();
        stage.setScene(new Scene(holder, 900, 800));
        stage.show();
        rebuild();
    }

    private void rebuild() {
        holder.getChildren().setAll(OperationPane.build(operation(), List::of,
                TagCatalog.of(document), SecuritySchemeCatalog.of(document),
                ResponseCatalog.of(document), (question, details) -> true, () -> { }));
    }

    private Operation operation() {
        return document.getPaths().getPathItem("/pets").getOperation(HttpMethod.GET);
    }

    @Test
    void offersEveryDefinedTag() {
        assertEquals(List.of("pets", "store", "ghost (undefined)"),
                tagCheckBoxes().stream().map(CheckBox::getText).toList());
    }

    @Test
    void aTagTheOperationUsesStartsChecked() {
        assertTrue(tagBox("pets").isSelected());
        assertFalse(tagBox("store").isSelected());
    }

    @Test
    void anUndefinedTagStartsCheckedAndMarked() {
        CheckBox ghost = tagBox("ghost (undefined)");

        assertTrue(ghost.isSelected());
        assertTrue(ghost.getStyleClass().contains("warning"));
    }

    @Test
    void togglingADefinedTagOnAddsItToTheOperation() {
        interact(() -> tagBox("store").setSelected(true));

        assertEquals(List.of("pets", "ghost", "store"), operation().getTags());
    }

    @Test
    void togglingADefinedTagOffRemovesItFromTheOperation() {
        interact(() -> tagBox("pets").setSelected(false));

        assertEquals(List.of("ghost"), operation().getTags());
    }

    @Test
    void uncheckingAnUndefinedTagDropsItAndItsRowDisappears() {
        interact(() -> tagBox("ghost (undefined)").setSelected(false));

        assertEquals(List.of("pets"), operation().getTags());
        assertFalse(tagCheckBoxes().stream().anyMatch(box -> box.getText().startsWith("ghost")));
    }

    @Test
    void anUndefinedTagIsNotAddedToTheRootListJustByShowingIt() {
        assertFalse(document.getTags().names().contains("ghost"),
                "showing the undefined tag must not declare it");
    }

    @Test
    void typingANewTagDeclaresItAndAppliesItToTheOperation() {
        interact(() -> {
            newTagField().setText("users");
            addTagButton().fire();
        });

        assertTrue(document.getTags().names().contains("users"), "should be declared at the root");
        assertEquals(List.of("pets", "ghost", "users"), operation().getTags());
    }

    /**
     * Enter must add too, not just the button: {@code TextField.setOnAction} is what fires on
     * Enter, so triggering that handler directly is the same thing a keypress would do, without
     * depending on real window focus for a robot-driven keypress to land.
     */
    @Test
    void pressingEnterInTheNewTagFieldAlsoAddsIt() {
        interact(() -> {
            newTagField().setText("users");
            newTagField().getOnAction().handle(new ActionEvent());
        });

        assertTrue(operation().getTags().contains("users"));
    }

    @Test
    void aBlankNewTagAddsNothing() {
        interact(() -> {
            newTagField().setText("   ");
            addTagButton().fire();
        });

        assertEquals(List.of("pets", "ghost"), operation().getTags());
    }

    private CheckBox tagBox(String text) {
        return tagCheckBoxes().stream()
                .filter(box -> text.equals(box.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no checkbox for " + text));
    }

    private List<CheckBox> tagCheckBoxes() {
        return lookup("#operation-tags").queryAs(VBox.class).getChildren().stream()
                .filter(CheckBox.class::isInstance)
                .map(CheckBox.class::cast)
                .toList();
    }

    private TextField newTagField() {
        return lookup("#operation-new-tag-name").queryAs(TextField.class);
    }

    private Button addTagButton() {
        return lookup("#operation-add-tag").queryButton();
    }
}
