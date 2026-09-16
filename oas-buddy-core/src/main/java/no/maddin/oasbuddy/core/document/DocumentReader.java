package no.maddin.oasbuddy.core.document;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DocumentReader {

    private DocumentReader() {
    }

    public static OasDocument read(Path file) throws IOException {
        DocumentFormat format = DocumentFormat.fromFileName(file.getFileName().toString());
        String content = Files.readString(file);
        return read(content, format);
    }

    public static OasDocument read(String content, DocumentFormat format) {
        ObjectMapper mapper = mapperFor(format);
        try {
            JsonNode tree = mapper.readTree(content);
            if (!(tree instanceof ObjectNode root)) {
                throw new DocumentParseException(
                        "Top level of an OpenAPI document must be an object", null);
            }
            return OasDocument.wrap(root, format);
        } catch (JsonProcessingException e) {
            JsonLocation location = e.getLocation();
            Integer line = location != null ? location.getLineNr() : null;
            Integer column = location != null ? location.getColumnNr() : null;
            throw new DocumentParseException(
                    "Could not parse " + format + " document: " + e.getOriginalMessage(), e, line, column);
        }
    }

    static ObjectMapper mapperFor(DocumentFormat format) {
        return switch (format) {
            case JSON -> new ObjectMapper();
            case YAML -> new ObjectMapper(YAMLFactory.builder()
                    .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                    .build());
        };
    }
}
