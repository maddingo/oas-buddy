package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.DocumentWriter;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Which constraints belong to which type, and how their values are read. */
class ConstraintTest {

    @Test
    void eachTypeHasItsOwnGroup() {
        assertAll(
                () -> assertEquals(List.of(Constraint.MINIMUM, Constraint.EXCLUSIVE_MINIMUM, Constraint.MAXIMUM,
                        Constraint.EXCLUSIVE_MAXIMUM, Constraint.MULTIPLE_OF), Constraint.forType("integer")),
                () -> assertEquals(Constraint.forType("integer"), Constraint.forType("number")),
                () -> assertEquals(List.of(Constraint.MIN_LENGTH, Constraint.MAX_LENGTH, Constraint.PATTERN),
                        Constraint.forType("string")),
                () -> assertEquals(List.of(Constraint.MIN_ITEMS, Constraint.MAX_ITEMS, Constraint.UNIQUE_ITEMS),
                        Constraint.forType("array")),
                () -> assertEquals(List.of(Constraint.MIN_PROPERTIES, Constraint.MAX_PROPERTIES),
                        Constraint.forType("object")),
                () -> assertEquals(List.of(), Constraint.forType("boolean")),
                () -> assertEquals(List.of(), Constraint.forType(null)));
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource({
            "MINIMUM, 1.5",
            "MINIMUM, -3",
            "MULTIPLE_OF, 0.01",
            "MIN_LENGTH, 0",
            "MAX_ITEMS, 10",
            "MIN_LENGTH, -1",
            "MIN_LENGTH, 1.5",
    })
    void numbersAreReadAsNumbersEvenOnesValidationWillReport(Constraint constraint, String text) {
        JsonNode value = constraint.parse(text);

        assertAll(
                () -> assertTrue(value.isNumber(), () -> "read as " + value.getNodeType()),
                () -> assertEquals(text, value.toString()));
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource({
            "MINIMUM, abc",
            "MINIMUM, 1 2",
            "MAX_ITEMS, '\"3\"'",
            "MAX_ITEMS, true",
    })
    void textThatIsNotOfTheKindIsRejectedRatherThanWrittenAsAString(Constraint constraint, String text) {
        assertThrows(IllegalArgumentException.class, () -> constraint.parse(text));
    }

    @Test
    void blankTextIsNoValue() {
        assertNull(Constraint.MINIMUM.parse(" "));
    }

    @Test
    void aPatternIsTakenLiterally() {
        assertEquals("^[a-z]+$", Constraint.PATTERN.parse("^[a-z]+$").asText());
    }

    @Test
    void changingTheTypeKeepsConstraintsThatNoLongerApplyAndSaysWhich() {
        Schema schema = OasDocument.newDocument(DocumentFormat.YAML).getComponents().getSchemas().addSchema("Code");
        schema.changeTypeTo("string");
        schema.setConstraint(Constraint.MAX_LENGTH, Constraint.MAX_LENGTH.parse("8"));
        schema.setConstraint(Constraint.PATTERN, Constraint.PATTERN.parse("^[A-Z]+$"));

        schema.changeTypeTo("integer");

        assertAll(
                () -> assertEquals(8, schema.getConstraint(Constraint.MAX_LENGTH).asInt()),
                () -> assertEquals(List.of(Constraint.MAX_LENGTH, Constraint.PATTERN), schema.constraintsNotApplying()));
    }

    @ParameterizedTest
    @EnumSource(DocumentFormat.class)
    void constraintsRoundTripAsNumbers(DocumentFormat format) {
        OasDocument document = OasDocument.newDocument(format);
        Schema price = document.getComponents().getSchemas().addSchema("Price");
        price.changeTypeTo("number");
        price.setConstraint(Constraint.MINIMUM, Constraint.MINIMUM.parse("0"));
        price.setConstraint(Constraint.EXCLUSIVE_MINIMUM, BooleanNode.TRUE);
        price.setConstraint(Constraint.MULTIPLE_OF, Constraint.MULTIPLE_OF.parse("0.01"));

        OasDocument reloaded = DocumentReader.read(DocumentWriter.write(document), format);
        Schema reloadedPrice = reloaded.getComponents().getSchemas().getSchema("Price");

        assertAll(
                () -> assertEquals(document.getRoot(), reloaded.getRoot()),
                () -> assertTrue(reloadedPrice.getConstraint(Constraint.MINIMUM).isNumber()),
                () -> assertTrue(reloadedPrice.getConstraint(Constraint.MULTIPLE_OF).isNumber()),
                () -> assertTrue(reloadedPrice.getConstraint(Constraint.EXCLUSIVE_MINIMUM).asBoolean()));
    }
}
