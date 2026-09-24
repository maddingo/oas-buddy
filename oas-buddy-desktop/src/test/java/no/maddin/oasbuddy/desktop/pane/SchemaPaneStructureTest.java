package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.Schema;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Array element types, property references and additionalProperties in the schema editor. */
class SchemaPaneStructureTest extends ApplicationTest {

    private static final int TYPE_COLUMN = 1;
    private static final int ITEMS_COLUMN = 2;
    private static final int FORMAT_COLUMN = 3;

    private OasDocument document;
    private StackPane holder;

    @Override
    public void start(Stage stage) {
        document = OasDocument.newDocument(DocumentFormat.YAML);
        var schemas = document.getComponents().getSchemas();
        Schema pet = schemas.addSchema("Pet");
        pet.changeTypeTo("object");
        pet.addProperty("name").changeTypeTo("string");
        Schema friends = pet.addProperty("friends");
        friends.changeTypeTo("array");
        friends.createItems().referTo("Pet");
        schemas.addSchema("Owner").changeTypeTo("object");
        schemas.addSchema("Tags").changeTypeTo("array");

        holder = new StackPane();
        stage.setScene(new Scene(holder, 1000, 700));
        stage.show();
        show("Pet");
    }

    private void show(String schemaName) {
        Node pane = SchemaPane.build(document, schemaName, (from, to) -> true, name -> { });
        interact(() -> holder.getChildren().setAll(pane));
    }

    @Test
    void thePropertyTypePickerOffersEveryDeclaredSchemaAsAReference() {
        assertAll(
                () -> assertTrue(propertyType("name").getItems().contains(TypeChoice.ref("Owner"))),
                () -> assertTrue(propertyType("name").getItems().contains(TypeChoice.ref("Pet")),
                        "a schema may refer to itself"),
                () -> assertTrue(propertyType("name").getItems().contains(TypeChoice.type("string"))));
    }

    @Test
    void pointingAPropertyAtASchemaWritesAReferenceInPlaceOfTheType() {
        interact(() -> propertyType("name").setValue(TypeChoice.ref("Owner")));

        Schema name = pet().getProperty("name");
        assertAll(
                () -> assertEquals("#/components/schemas/Owner", name.getRef()),
                () -> assertNull(name.getType()),
                () -> assertNull(cell("name", FORMAT_COLUMN), "a reference has no format of its own"));
    }

    @Test
    void aLoadedArrayPropertyShowsItsElementType() {
        assertAll(
                () -> assertEquals(TypeChoice.type("array"), propertyType("friends").getValue()),
                () -> assertEquals(TypeChoice.ref("Pet"), propertyItems("friends").getValue()));
    }

    @Test
    void onlyAnArrayPropertyHasAnElementTypePicker() {
        assertNull(cell("name", ITEMS_COLUMN));
    }

    @Test
    void makingAPropertyAnArrayRevealsTheElementTypePicker() {
        interact(() -> propertyType("name").setValue(TypeChoice.type("array")));
        interact(() -> propertyItems("name").setValue(TypeChoice.type("string")));

        assertEquals("string", pet().getProperty("name").getItems().getType());
    }

    @Test
    void anElementTypeCanBeAReference() {
        interact(() -> propertyItems("friends").setValue(TypeChoice.ref("Owner")));

        assertEquals("Owner", pet().getProperty("friends").getItems().getReferencedSchemaName());
    }

    @Test
    void changingAPropertyAwayFromArrayDropsItsItems() {
        interact(() -> propertyType("friends").setValue(TypeChoice.type("string")));

        assertAll(
                () -> assertNull(pet().getProperty("friends").getItems()),
                () -> assertNull(cell("friends", ITEMS_COLUMN)),
                () -> assertInstanceOf(TextField.class, cell("friends", FORMAT_COLUMN)));
    }

    @Test
    void anArraySchemaShowsItsItemsRowAndAnObjectDoesNot() {
        assertFalse(shown("#schema-items"), "an object has no items");

        show("Tags");
        assertTrue(shown("#schema-items"), "an array needs its items");
    }

    @Test
    void choosingTheElementTypeOfAnArraySchemaWritesIt() {
        show("Tags");

        interact(() -> itemsBox().setValue(TypeChoice.ref("Owner")));

        assertEquals("Owner", schema("Tags").getItems().getReferencedSchemaName());
    }

    @Test
    void turningAnArraySchemaIntoAnObjectDropsItsItemsAndShowsAdditionalProperties() {
        show("Tags");
        interact(() -> itemsBox().setValue(TypeChoice.type("string")));
        assertFalse(shown("#schema-additional-properties"), "an array has no additionalProperties");

        interact(() -> schemaType().setValue("object"));

        assertAll(
                () -> assertNull(schema("Tags").getItems()),
                () -> assertFalse(shown("#schema-items")),
                () -> assertTrue(shown("#schema-additional-properties")));
    }

    @Test
    void forbiddingAdditionalPropertiesWritesFalse() {
        interact(() -> additionalProperties().setValue(SchemaPane.AdditionalProperties.NONE));

        assertEquals(Boolean.FALSE, pet().getAdditionalPropertiesAllowed());
    }

    @Test
    void typedAdditionalPropertiesRevealATypePickerAndWriteASchema() {
        assertFalse(shown("#schema-additional-properties-type"));

        interact(() -> additionalProperties().setValue(SchemaPane.AdditionalProperties.TYPED));
        interact(() -> additionalPropertiesType().setValue(TypeChoice.type("integer")));

        assertAll(
                () -> assertTrue(shown("#schema-additional-properties-type")),
                () -> assertEquals("integer", pet().getAdditionalPropertiesSchema().getType()));
    }

    @Test
    void leavingAdditionalPropertiesUnspecifiedRemovesTheKey() {
        interact(() -> additionalProperties().setValue(SchemaPane.AdditionalProperties.TYPED));
        interact(() -> additionalPropertiesType().setValue(TypeChoice.type("integer")));

        interact(() -> additionalProperties().setValue(SchemaPane.AdditionalProperties.UNSPECIFIED));

        assertFalse(document.getRoot().get("components").get("schemas").get("Pet").has("additionalProperties"));
    }

    @Test
    void openingASchemaAddsNothingToIt() {
        show("Owner");
        show("Tags");

        assertAll(
                () -> assertFalse(document.getRoot().get("components").get("schemas").get("Owner").has("additionalProperties")),
                () -> assertFalse(document.getRoot().get("components").get("schemas").get("Tags").has("items")));
    }

    /** Visible itself and inside visible ancestors: a row is hidden as a whole, not control by control. */
    private boolean shown(String id) {
        for (Node node = lookup(id).query(); node != null; node = node.getParent()) {
            if (!node.isVisible()) {
                return false;
            }
        }
        return true;
    }

    private Schema pet() {
        return schema("Pet");
    }

    private Schema schema(String name) {
        return document.getComponents().getSchemas().getSchema(name);
    }

    private Node cell(String property, int column) {
        return GridPanes.cellInRowOf(lookup("#schema-properties").queryAs(GridPane.class), property, column);
    }

    @SuppressWarnings("unchecked")
    private ComboBox<TypeChoice> propertyType(String property) {
        return (ComboBox<TypeChoice>) cell(property, TYPE_COLUMN);
    }

    @SuppressWarnings("unchecked")
    private ComboBox<TypeChoice> propertyItems(String property) {
        return (ComboBox<TypeChoice>) cell(property, ITEMS_COLUMN);
    }

    @SuppressWarnings("unchecked")
    private ComboBox<String> schemaType() {
        return lookup("#schema-type").queryAs(ComboBox.class);
    }

    @SuppressWarnings("unchecked")
    private ComboBox<TypeChoice> itemsBox() {
        return lookup("#schema-items").queryAs(ComboBox.class);
    }

    @SuppressWarnings("unchecked")
    private ComboBox<SchemaPane.AdditionalProperties> additionalProperties() {
        return lookup("#schema-additional-properties").queryAs(ComboBox.class);
    }

    @SuppressWarnings("unchecked")
    private ComboBox<TypeChoice> additionalPropertiesType() {
        return lookup("#schema-additional-properties-type").queryAs(ComboBox.class);
    }
}
