package no.maddin.oasbuddy.core.document;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentWriterTest {

    @Test
    void jsonKeysAreFollowedByASpaceButNotPrecededByOne() throws IOException {
        OasDocument document = DocumentReader.read(
                "{\"openapi\":\"3.0.3\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"paths\":{}}",
                DocumentFormat.JSON);

        String written = DocumentWriter.write(document);

        assertTrue(written.contains("\"openapi\": \"3.0.3\""), written);
        assertTrue(written.contains("\"paths\": {"), written);
        assertFalse(written.contains("\" :"), written);
    }
}
