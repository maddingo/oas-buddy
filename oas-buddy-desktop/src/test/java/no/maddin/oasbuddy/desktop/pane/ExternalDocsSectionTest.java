package no.maddin.oasbuddy.desktop.pane;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import no.maddin.oasbuddy.core.model.Operation;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The one section, in each of the four places it appears. */
class ExternalDocsSectionTest extends ApplicationTest {

    private OasDocument document;
    private Operation operation;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getTags().add("pets");
        document.getComponents().getSchemas().addSchema("Pet");
        operation = document.getPaths().addPath("/pets").addOperation(HttpMethod.GET);
        show(InfoPane.build(document.getInfo(), document.getExternalDocs()));
    }

    private void show(Node pane) {
        interact(() -> {
            stage.setScene(new Scene(new StackPane(pane), 900, 900));
            stage.show();
        });
    }

    private TextField field(String styleClass) {
        return lookup("." + styleClass).queryAs(TextField.class);
    }

    @Test
    void theDocumentRootCanSetAndClearExternalDocs() {
        assertFalse(document.getRoot().has("externalDocs"), "showing the pane adds nothing");

        interact(() -> field(ExternalDocsSection.URL_CLASS).setText("https://example.com"));
        assertEquals("https://example.com", document.getExternalDocs().getUrl());

        interact(() -> field(ExternalDocsSection.URL_CLASS).setText(""));
        assertFalse(document.getRoot().has("externalDocs"), "clearing removes the key");
    }

    @Test
    void aTagCanSetExternalDocs() {
        show(TagsPane.build(document, () -> { }, name -> { }));

        interact(() -> field(ExternalDocsSection.DESCRIPTION_CLASS).setText("Read more"));

        assertEquals("Read more", document.getTags().get("pets").getExternalDocs().getDescription());
    }

    @Test
    void anOperationCanSetExternalDocs() {
        show(OperationPane.build(operation, List::of, TagCatalog.of(document), SecuritySchemeCatalog.of(document),
                ComponentCatalog.of(document), (question, details) -> true, () -> { }));

        interact(() -> field(ExternalDocsSection.URL_CLASS).setText("https://op"));

        assertEquals("https://op", operation.getExternalDocs().getUrl());
    }

    @Test
    void aSchemaCanSetExternalDocs() {
        show(SchemaPane.build(document, "Pet", (from, to) -> true, name -> { }));

        interact(() -> field(ExternalDocsSection.URL_CLASS).setText("https://schema"));

        assertEquals("https://schema", document.getComponents().getSchemas().getSchema("Pet")
                .getExternalDocs().getUrl());
    }

    @Test
    void externalDocsThatIsNotAnObjectIsShownButNotRewritten() {
        document.getRoot().put("externalDocs", "oops");

        show(InfoPane.build(document.getInfo(), document.getExternalDocs()));

        assertTrue(lookup("." + ExternalDocsSection.URL_CLASS).tryQuery().isEmpty());
        assertEquals("oops", document.getRoot().get("externalDocs").asText());
    }
}
