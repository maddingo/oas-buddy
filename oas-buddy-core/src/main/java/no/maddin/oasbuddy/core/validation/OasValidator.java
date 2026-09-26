package no.maddin.oasbuddy.core.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import no.maddin.oasbuddy.core.document.OasDocument;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.util.ArrayList;
import java.util.List;

public final class OasValidator {

    /** Reads YAML and, YAML being a superset of it, JSON. */
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    public ValidationResult validate(OasDocument document) {
        return validate(document.getRoot().toString());
    }

    public ValidationResult validate(String content) {
        ParseOptions options = new ParseOptions();
        options.setResolve(false);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, null, options);
        List<ValidationMessage> messages = new ArrayList<>();
        result.getMessages().forEach(message -> messages.add(new ValidationMessage(ValidationSeverity.ERROR, message)));
        messages.addAll(checkConstraints(content));
        messages.addAll(checkTags(content));
        messages.addAll(checkReferences(content));
        return new ValidationResult(messages);
    }

    /** Content swagger-parser could not read has already been reported by it; nothing to add. */
    private static List<ValidationMessage> checkConstraints(String content) {
        try {
            return ConstraintCheck.check(YAML.readTree(content));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    /** Content swagger-parser could not read has already been reported by it; nothing to add. */
    private static List<ValidationMessage> checkReferences(String content) {
        try {
            return ReferenceCheck.check(YAML.readTree(content));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    /** Content swagger-parser could not read has already been reported by it; nothing to add. */
    private static List<ValidationMessage> checkTags(String content) {
        try {
            return TagCheck.check(YAML.readTree(content));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
