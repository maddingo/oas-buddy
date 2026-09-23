package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.DocumentWriter;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** enum, default, example and the descriptive fields of a schema. */
class SchemaValuesTest {

    @ParameterizedTest(name = "{0} as {1}")
    @CsvSource(delimiter = '|', value = {
            "42          | integer | true  | 42",
            "4.5         | integer | false | 4.5",
            "4.5         | number  | true  | 4.5",
            "true        | boolean | true  | true",
            "yes         | boolean | false | yes",
            "42          | string  | false | 42",
            "{\"a\": 1}  | object  | true  | {\"a\":1}",
            "[1, 2]      | array   | true  | [1,2]",
            "1 2         | integer | false | 1 2",
            "42          |         | true  | 42",
            "hello       |         | false | hello",
    })
    void textIsReadAsTheSchemasTypeOrElseKeptLiterally(String text, String type, boolean typed, String shown) {
        JsonNode value = SchemaValues.parse(text, type);

        assertAll(
                () -> assertEquals(!typed, value.isTextual(), "typed as " + value.getNodeType()),
                () -> assertEquals(shown, SchemaValues.display(value)));
    }

    @Test
    void blankTextIsNoValue() {
        assertNull(SchemaValues.parse("  ", "integer"));
    }

    @Test
    void untouchedSchemasHaveNoFlagsAndNoValues() {
        Schema schema = newSchema("integer");

        assertAll(
                () -> assertFalse(schema.isNullable()),
                () -> assertFalse(schema.isReadOnly()),
                () -> assertFalse(schema.isWriteOnly()),
                () -> assertFalse(schema.isDeprecated()),
                () -> assertNull(schema.getDefault()),
                () -> assertNull(schema.getExample()),
                () -> assertTrue(schema.getEnum().isEmpty()));
    }

    @Test
    void flagsAreOnlyWrittenWhenTrue() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        Schema schema = document.getComponents().getSchemas().addSchema("Id");

        schema.setReadOnly(true);
        schema.setDeprecated(true);
        schema.setDeprecated(false);
        schema.setNullable(false);

        assertEquals(List.of("readOnly"), fieldNames(raw(document, "Id")));
    }

    @Test
    void anEmptyEnumRemovesTheKey() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        Schema schema = document.getComponents().getSchemas().addSchema("Status");
        schema.setEnum(List.of(SchemaValues.parse("a", "string")));

        schema.setEnum(List.of());

        assertFalse(raw(document, "Status").has("enum"));
    }

    @ParameterizedTest
    @EnumSource(DocumentFormat.class)
    void typedValuesRoundTripAsTheirType(DocumentFormat format) {
        OasDocument document = OasDocument.newDocument(format);
        Schema size = document.getComponents().getSchemas().addSchema("Size");
        size.changeTypeTo("integer");
        size.setTitle("Size");
        size.setEnum(List.of(parse("1", size), parse("2", size), parse("3", size)));
        size.setDefault(parse("2", size));
        size.setExample(parse("3", size));
        size.setNullable(true);
        Schema code = document.getComponents().getSchemas().addSchema("Code");
        code.changeTypeTo("string");
        code.setDefault(parse("007", code));

        OasDocument reloaded = DocumentReader.read(DocumentWriter.write(document), format);
        Schema reloadedSize = reloaded.getComponents().getSchemas().getSchema("Size");
        Schema reloadedCode = reloaded.getComponents().getSchemas().getSchema("Code");

        assertAll(
                () -> assertEquals(document.getRoot(), reloaded.getRoot()),
                () -> assertEquals("Size", reloadedSize.getTitle()),
                () -> assertTrue(reloadedSize.getDefault().isInt(), "default written as a string"),
                () -> assertTrue(reloadedSize.getExample().isInt(), "example written as a string"),
                () -> assertTrue(reloadedSize.getEnum().stream().allMatch(JsonNode::isInt)),
                () -> assertTrue(reloadedSize.isNullable()),
                () -> assertEquals("007", reloadedCode.getDefault().asText(),
                        "a string that looks like a number must stay a string"));
    }

    private static JsonNode parse(String text, Schema schema) {
        return SchemaValues.parse(text, schema.getType());
    }

    private static Schema newSchema(String type) {
        Schema schema = OasDocument.newDocument(DocumentFormat.YAML).getComponents().getSchemas().addSchema("S");
        schema.changeTypeTo(type);
        return schema;
    }

    private static ObjectNode raw(OasDocument document, String schemaName) {
        return (ObjectNode) document.getRoot().get("components").get("schemas").get(schemaName);
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
