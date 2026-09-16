package no.maddin.oasbuddy.core.validation;

import no.maddin.oasbuddy.core.document.OasDocument;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.util.List;

public final class OasValidator {

    public ValidationResult validate(OasDocument document) {
        return validate(document.getRoot().toString());
    }

    public ValidationResult validate(String content) {
        ParseOptions options = new ParseOptions();
        options.setResolve(false);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, null, options);
        List<ValidationMessage> messages = result.getMessages().stream()
                .map(message -> new ValidationMessage(ValidationSeverity.ERROR, message))
                .toList();
        return new ValidationResult(messages);
    }
}
