package no.maddin.oasbuddy.core.document;

public class DocumentParseException extends RuntimeException {

    private final Integer line;
    private final Integer column;

    public DocumentParseException(String message, Throwable cause) {
        this(message, cause, null, null);
    }

    public DocumentParseException(String message, Throwable cause, Integer line, Integer column) {
        super(message, cause);
        this.line = line;
        this.column = column;
    }

    public Integer getLine() {
        return line;
    }

    public Integer getColumn() {
        return column;
    }
}
