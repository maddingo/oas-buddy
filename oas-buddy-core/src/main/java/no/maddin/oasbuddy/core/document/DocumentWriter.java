package no.maddin.oasbuddy.core.document;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DocumentWriter {

    private DocumentWriter() {
    }

    public static void write(OasDocument document, Path file) throws IOException {
        Files.writeString(file, write(document));
    }

    public static String write(OasDocument document) {
        DocumentFormat format = document.getFormat();
        ObjectMapper mapper = DocumentReader.mapperFor(format);
        try {
            if (format == DocumentFormat.JSON) {
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(document.getRoot());
            }
            return mapper.writeValueAsString(document.getRoot());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not serialize document", e);
        }
    }
}
