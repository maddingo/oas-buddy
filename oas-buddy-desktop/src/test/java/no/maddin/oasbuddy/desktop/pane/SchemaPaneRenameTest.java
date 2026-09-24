package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.Schema;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
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

/** The rename controls on a schema's own editor: its header, and each property row. */
class SchemaPaneRenameTest extends ApplicationTest {

    private final List<String[]> renameRequests = new ArrayList<>();
    private OasDocument document;
    private StackPane holder;

    @Override
    public void start(Stage stage) {
        document = OasDocument.newDocument(DocumentFormat.YAML);
        var schemas = document.getComponents().getSchemas();
        Schema pet = schemas.addSchema("Pet");
        pet.setType("object");
        pet.addProperty("name").setType("string");
        pet.addProperty("age").setType("integer");
        pet.setRequired(List.of("name"));
        schemas.addSchema("Owner").setType("object");

        holder = new StackPane();
        stage.setScene(new Scene(holder, 900, 700));
        stage.show();
        show("Pet");
    }

    private void show(String schemaName) {
        Node pane = SchemaPane.build(document, schemaName, this::onRenameSchema, name -> { });
        interact(() -> holder.getChildren().setAll(pane));
    }

    private boolean onRenameSchema(String from, String to) {
        renameRequests.add(new String[] {from, to});
        return document.getComponents().getSchemas().renameSchema(from, to);
    }

    @Test
    void showsTheCurrentSchemaName() {
        assertEquals("Pet", nameField().getText());
    }

    @Test
    void committingANewNameReportsTheRequest() {
        commit(nameField(), "Animal");

        assertEquals(1, renameRequests.size());
        assertEquals("Pet", renameRequests.get(0)[0]);
        assertEquals("Animal", renameRequests.get(0)[1]);
        assertEquals("Animal", nameField().getText());
    }

    @Test
    void collidingWithAnExistingSchemaIsRefusedWithoutReportingIt() {
        commit(nameField(), "Owner");

        assertTrue(renameRequests.isEmpty(), "a collision is caught before the callback is asked");
        assertEquals("Pet", nameField().getText(), "the field reverts to the old name");
    }

    @Test
    void aBlankSchemaNameIsRefused() {
        commit(nameField(), "  ");

        assertTrue(renameRequests.isEmpty());
        assertEquals("Pet", nameField().getText());
    }

    @Test
    void renamingAPropertyKeepsItsTypeAndUpdatesRequired() {
        commit(propertyNameField("name"), "label");

        Schema pet = document.getComponents().getSchemas().getSchema("Pet");
        assertEquals(List.of("label", "age"), pet.propertyNames());
        assertEquals("string", pet.getProperty("label").getType());
        assertEquals(List.of("label"), pet.getRequired());
    }

    @Test
    void renamingAPropertyOntoAnExistingOneIsRefused() {
        commit(propertyNameField("name"), "age");

        Schema pet = document.getComponents().getSchemas().getSchema("Pet");
        assertEquals(List.of("name", "age"), pet.propertyNames());
        assertEquals(List.of("name"), pet.getRequired());
    }

    private void commit(TextField field, String newName) {
        interact(() -> {
            field.setText(newName);
            field.fireEvent(new ActionEvent());
        });
    }

    private TextField nameField() {
        return lookup("#schema-name").queryAs(TextField.class);
    }

    private TextField propertyNameField(String property) {
        return (TextField) GridPanes.cellInRowOf(lookup("#schema-properties").queryAs(GridPane.class), property, 0);
    }
}
