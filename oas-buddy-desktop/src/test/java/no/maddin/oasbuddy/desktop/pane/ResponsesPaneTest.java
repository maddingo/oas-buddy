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

class ResponsesPaneTest extends ApplicationTest {

    private final List<String> removalRequests = new ArrayList<>();
    private OasDocument document;

    @Override
    public void start(Stage stage) {
        document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getResponses().addResponse("NotFound").setDescription("Not here");
        document.getComponents().getResponses().addResponse("Error").setDescription("Broken");

        Node pane = ResponsesPane.build(document, () -> { }, removalRequests::add);
        stage.setScene(new Scene(new StackPane(pane), 900, 600));
        stage.show();
    }

    @Test
    void listsOneRemovableRowPerResponse() {
        assertEquals(List.of("NotFound", "Error"), GridPanes.entries(list()));
    }

    @Test
    void askingToRemoveAResponseReportsItByName() {
        interact(() -> GridPanes.buttonInRowOf(list(), "Error").fire());

        assertEquals(List.of("Error"), removalRequests);
    }

    @Test
    void removalIsNotAppliedByThePaneItself() {
        interact(() -> GridPanes.buttonInRowOf(list(), "NotFound").fire());

        assertTrue(document.getComponents().getResponses().names().contains("NotFound"),
                "the pane must delegate, not delete");
    }

    @Test
    void aNewResponseStartsWithTheDescriptionOasRequires() {
        interact(() -> {
            lookup("#new-response-name").queryAs(TextField.class).setText("Unauthorized");
            lookup("#add-response").queryButton().fire();
        });

        assertEquals("", document.getComponents().getResponses().getResponse("Unauthorized").getDescription());
    }

    @Test
    void aBlankNameAddsNothing() {
        Button add = lookup("#add-response").queryButton();

        interact(() -> {
            lookup("#new-response-name").queryAs(TextField.class).setText("  ");
            add.fire();
        });

        assertEquals(List.of("NotFound", "Error"), document.getComponents().getResponses().names());
    }

    private GridPane list() {
        return lookup("#responses-list").queryAs(GridPane.class);
    }
}
