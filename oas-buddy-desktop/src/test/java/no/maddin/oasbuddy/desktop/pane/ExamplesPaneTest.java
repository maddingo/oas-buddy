package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExamplesPaneTest extends ApplicationTest {

    private final List<String> removalRequests = new ArrayList<>();
    private OasDocument document;

    @Override
    public void start(Stage stage) {
        document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getExamples().addExample("Cat").setSummary("A cat");
        document.getComponents().getExamples().addExample("Dog").setSummary("A dog");

        Node pane = ExamplesPane.build(document, () -> { }, removalRequests::add);
        stage.setScene(new Scene(new StackPane(pane), 900, 600));
        stage.show();
    }

    @Test
    void listsOneRemovableRowPerExample() {
        assertEquals(List.of("Cat", "Dog"), GridPanes.entries(list()));
    }

    @Test
    void askingToRemoveAnExampleReportsItByName() {
        interact(() -> GridPanes.buttonInRowOf(list(), "Dog").fire());

        assertEquals(List.of("Dog"), removalRequests);
    }

    @Test
    void removalIsNotAppliedByThePaneItself() {
        interact(() -> GridPanes.buttonInRowOf(list(), "Cat").fire());

        assertTrue(document.getComponents().getExamples().names().contains("Cat"),
                "the pane must delegate, not delete");
    }

    @Test
    void addsAnEmptyExample() {
        interact(() -> {
            lookup("#new-example-name").queryAs(TextField.class).setText("Bird");
            lookup("#add-component-example").queryButton().fire();
        });

        assertTrue(document.getComponents().getExamples().getExample("Bird").isEmpty());
    }

    @Test
    void aBlankNameAddsNothing() {
        Button add = lookup("#add-component-example").queryButton();

        interact(() -> {
            lookup("#new-example-name").queryAs(TextField.class).setText("  ");
            add.fire();
        });

        assertEquals(List.of("Cat", "Dog"), document.getComponents().getExamples().names());
    }

    private GridPane list() {
        return lookup("#examples-list").queryAs(GridPane.class);
    }
}
