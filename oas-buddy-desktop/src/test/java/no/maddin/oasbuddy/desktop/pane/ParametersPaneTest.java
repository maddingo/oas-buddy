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

class ParametersPaneTest extends ApplicationTest {

    private final List<String> removalRequests = new ArrayList<>();
    private OasDocument document;

    @Override
    public void start(Stage stage) {
        document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getParameters().addParameter("Limit", "limit", "query");
        document.getComponents().getParameters().addParameter("Trace", "X-Trace", "header");

        Node pane = ParametersPane.build(document, () -> { }, removalRequests::add);
        stage.setScene(new Scene(new StackPane(pane), 900, 600));
        stage.show();
    }

    @Test
    void listsOneRemovableRowPerParameter() {
        assertEquals(List.of("Limit", "Trace"), GridPanes.entries(list()));
    }

    @Test
    void askingToRemoveAParameterReportsItByKey() {
        interact(() -> GridPanes.buttonInRowOf(list(), "Trace").fire());

        assertEquals(List.of("Trace"), removalRequests);
    }

    @Test
    void removalIsNotAppliedByThePaneItself() {
        interact(() -> GridPanes.buttonInRowOf(list(), "Limit").fire());

        assertTrue(document.getComponents().getParameters().names().contains("Limit"),
                "the pane must delegate, not delete");
    }

    @Test
    void aNewParameterIsNamedAfterItsKeyAndStartsInTheQuery() {
        interact(() -> {
            lookup("#new-parameter-name").queryAs(TextField.class).setText("offset");
            lookup("#add-component-parameter").queryButton().fire();
        });

        var added = document.getComponents().getParameters().getParameter("offset");
        assertEquals("offset", added.getName());
        assertEquals("query", added.getIn());
    }

    @Test
    void aBlankNameAddsNothing() {
        Button add = lookup("#add-component-parameter").queryButton();

        interact(() -> {
            lookup("#new-parameter-name").queryAs(TextField.class).setText("  ");
            add.fire();
        });

        assertEquals(List.of("Limit", "Trace"), document.getComponents().getParameters().names());
    }

    private GridPane list() {
        return lookup("#parameters-list").queryAs(GridPane.class);
    }
}
