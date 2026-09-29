package no.maddin.oasbuddy.core.document;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DocumentWriter {

    /**
     * Jackson's default puts a space on both sides of the colon ({@code "key" : value}); the
     * convention in hand-written JSON, and so in the diffs this editor exists to keep clean, is
     * a space after it only.
     */
    private static final DefaultPrettyPrinter JSON_PRINTER = new DefaultPrettyPrinter()
            .withSeparators(Separators.createDefaultInstance()
                    .withObjectFieldValueSpacing(Separators.Spacing.AFTER));

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
                return mapper.writer(JSON_PRINTER).writeValueAsString(document.getRoot());
            }
            return mapper.writeValueAsString(document.getRoot());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not serialize document", e);
        }
    }
}
