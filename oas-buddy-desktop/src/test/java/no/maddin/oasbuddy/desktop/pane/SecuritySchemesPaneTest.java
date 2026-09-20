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

class SecuritySchemesPaneTest extends ApplicationTest {

    private final List<String> removalRequests = new ArrayList<>();
    private OasDocument document;

    @Override
    public void start(Stage stage) {
        document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getSecuritySchemes().addScheme("ApiKeyAuth").setType("apiKey");
        document.getComponents().getSecuritySchemes().addScheme("OAuth2Auth").setType("oauth2");

        Node pane = SecuritySchemesPane.build(document, () -> { }, removalRequests::add);
        stage.setScene(new Scene(new StackPane(pane), 900, 600));
        stage.show();
    }

    @Test
    void listsOneRemovableRowPerScheme() {
        assertEquals(List.of("ApiKeyAuth", "OAuth2Auth"), GridPanes.entries(list()));
    }

    @Test
    void askingToRemoveASchemeReportsThatSchemeByName() {
        interact(() -> GridPanes.buttonInRowOf(list(), "OAuth2Auth").fire());

        assertEquals(List.of("OAuth2Auth"), removalRequests);
    }

    @Test
    void removalIsNotAppliedByThePaneItself() {
        interact(() -> GridPanes.buttonInRowOf(list(), "ApiKeyAuth").fire());

        assertTrue(GridPanes.entries(list()).contains("ApiKeyAuth"), "the pane must delegate, not delete");
    }

    @Test
    void aNewSchemeStartsOutAsAnApiKey() {
        TextField nameField = lookup("#new-security-scheme-name").queryAs(TextField.class);
        Button add = lookup("#add-security-scheme").queryButton();

        interact(() -> {
            nameField.setText("BearerAuth");
            add.fire();
        });

        assertEquals("apiKey",
                document.getComponents().getSecuritySchemes().getScheme("BearerAuth").getType());
    }

    @Test
    void aBlankNameAddsNothing() {
        Button add = lookup("#add-security-scheme").queryButton();

        interact(() -> {
            lookup("#new-security-scheme-name").queryAs(TextField.class).setText("   ");
            add.fire();
        });

        assertEquals(List.of("ApiKeyAuth", "OAuth2Auth"),
                document.getComponents().getSecuritySchemes().names());
    }

    private GridPane list() {
        return lookup("#security-schemes-list").queryAs(GridPane.class);
    }
}
