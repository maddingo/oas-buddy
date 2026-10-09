package no.maddin.oasbuddy.desktop.pane;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.ApiResponse;
import no.maddin.oasbuddy.core.model.HttpMethod;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinksEditorTest extends ApplicationTest {

    private final AtomicBoolean confirmAnswer = new AtomicBoolean(true);
    private final AtomicBoolean asked = new AtomicBoolean();
    private OasDocument document;
    private ApiResponse created;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getLinks().addLink("Shared").setOperationId("getPet");
        var pets = document.getPaths().addPath("/pets");
        pets.addOperation(HttpMethod.POST).setOperationId("addPet");
        created = pets.getOperation(HttpMethod.POST).getResponses().addResponse("201");
        created.setDescription("created");
        document.getPaths().addPath("/pets/{id}").addOperation(HttpMethod.GET).setOperationId("getPet");
        showOperation();
    }

    private void showOperation() {
        var operation = document.getPaths().getPathItem("/pets").getOperation(HttpMethod.POST);
        show(OperationPane.build(operation, List::of, TagCatalog.of(document), SecuritySchemeCatalog.of(document),
                ComponentCatalog.of(document), (q, d) -> {
                    asked.set(true);
                    return confirmAnswer.get();
                }, () -> { }));
    }

    private void show(Node pane) {
        interact(() -> {
            stage.setScene(new Scene(new StackPane(pane), 1200, 1000));
            stage.show();
        });
    }

    private Node byClass(String styleClass) {
        return lookup("." + styleClass).query();
    }

    private Button button(String text) {
        return lookup((Node n) -> n instanceof Button b && text.equals(b.getText())).queryButton();
    }

    private void addLink(String name) {
        interact(() -> ((TextField) byClass(LinksEditor.NEW_NAME_CLASS)).setText(name));
        interact(() -> button("Add link").fire());
    }

    @Test
    void showingAResponseAddsNoLinksKey() {
        assertFalse(document.getRoot().get("paths").get("/pets").get("post").get("responses").get("201").has("links"));
    }

    @Test
    void aLinkPicksItsOperationFromTheDeclaredIds() {
        addLink("GetPet");

        @SuppressWarnings("unchecked")
        ComboBox<String> operation = (ComboBox<String>) byClass(LinkForm.OPERATION_ID_CLASS);
        assertEquals(List.of("addPet", "getPet"), operation.getItems());

        interact(() -> operation.getEditor().setText("getPet"));
        interact(() -> ((TextField) byClass(LinkForm.DESCRIPTION_CLASS)).setText("The new pet"));
        interact(() -> ((TextField) byClass(LinkForm.REQUEST_BODY_CLASS)).setText("$request.body"));

        var link = created.getLinks().get("GetPet");
        assertEquals("getPet", link.getOperationId());
        assertEquals("The new pet", link.getDescription());
        assertEquals("$request.body", link.getRequestBody().asText());
    }

    @Test
    void aLinkToAMissingOperationIsAcceptedNotBlocked() {
        addLink("Ghost");
        interact(() -> ((ComboBox<?>) byClass(LinkForm.OPERATION_ID_CLASS)).getEditor().setText("nowhere"));

        assertEquals("nowhere", created.getLinks().get("Ghost").getOperationId());
    }

    @Test
    void parametersAreAddedEditedAndRemoved() {
        addLink("GetPet");
        interact(() -> ((TextField) byClass(LinkForm.NEW_PARAMETER_CLASS)).setText("id"));
        interact(() -> ((Button) byClass(LinkForm.ADD_PARAMETER_CLASS)).fire());
        interact(() -> ((TextField) byClass(LinkForm.PARAMETER_VALUE_CLASS)).setText("$response.body#/id"));

        var parameters = created.getLinks().get("GetPet").getParameters();
        assertEquals(List.of("id"), parameters.names());
        assertEquals("$response.body#/id", parameters.get("id").asText());

        interact(() -> ((Button) byClass(LinkForm.PARAMETER_VALUE_CLASS).getParent().lookup(".button")).fire());
        assertTrue(created.getLinks().get("GetPet").getParameters().names().isEmpty());
    }

    @Test
    void aResponseCanReferToAReusableLinkAndSwitchingToInlineCopiesIt() {
        @SuppressWarnings("unchecked")
        ComboBox<String> components = (ComboBox<String>) byClass(LinksEditor.REFERENCE_CLASS);
        interact(() -> components.setValue("Shared"));
        interact(() -> button("Add reference").fire());

        assertEquals("Shared", created.getLinks().get("Shared").getReferencedLinkName());

        @SuppressWarnings("unchecked")
        ComboBox<SourceChoice> picker = (ComboBox<SourceChoice>) byClass(LinksEditor.SOURCE_CLASS);
        interact(() -> picker.setValue(SourceChoice.INLINE));

        assertFalse(created.getLinks().get("Shared").isReference());
        assertEquals("getPet", created.getLinks().get("Shared").getOperationId());
        assertFalse(asked.get(), "ref -> inline loses nothing, so it never asks");
    }

    @Test
    void replacingAnInlineLinkWithAReferenceAsksAndDecliningKeepsIt() {
        created.getLinks().add("Mine").setOperationId("addPet");
        showOperation();
        confirmAnswer.set(false);

        @SuppressWarnings("unchecked")
        ComboBox<SourceChoice> picker = (ComboBox<SourceChoice>) byClass(LinksEditor.SOURCE_CLASS);
        interact(() -> picker.setValue(new SourceChoice("Shared")));

        assertTrue(asked.get());
        assertEquals(SourceChoice.INLINE, picker.getValue(), "declining reverts the picker");
        assertEquals("addPet", created.getLinks().get("Mine").getOperationId());
    }

    @Test
    void aLinkThatIsNotAnObjectIsShownAndCanBeRemovedButNotEdited() {
        ((com.fasterxml.jackson.databind.node.ObjectNode) document.getRoot().get("paths").get("/pets").get("post")
                .get("responses").get("201")).putObject("links").put("Odd", "~");
        showOperation();

        assertTrue(lookup((Node n) -> n instanceof Label l && "(not an object)".equals(l.getText()))
                .tryQuery().isPresent());

        Node row = lookup("." + LinksEditor.ROW_CLASS).query();
        interact(() -> ((Button) row.lookup(".button")).fire());
        assertFalse(document.getRoot().get("paths").get("/pets").get("post").get("responses").get("201")
                .has("links"));
    }

    @Test
    void theComponentResponseEditorHasTheSameSection() {
        document.getComponents().getResponses().addResponse("Created").setDescription("created");
        show(ResponsePane.build(document, "Created", List::of, (q, d) -> true, name -> { }));

        addLink("Follow");

        assertEquals(List.of("Follow"),
                document.getComponents().getResponses().getResponse("Created").getLinks().names());
    }

    @Test
    void theComponentLinkEditorEditsItsFields() {
        show(LinkPane.build(document, "Shared", name -> { }));

        interact(() -> ((TextField) byClass(LinkForm.OPERATION_REF_CLASS)).setText("#/paths/~1pets/get"));

        assertEquals("#/paths/~1pets/get", document.getComponents().getLinks().getLink("Shared").getOperationRef());
    }
}
