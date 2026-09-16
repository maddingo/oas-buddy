package no.maddin.oasbuddy.core.validation;

import java.util.List;

public record ValidationResult(List<ValidationMessage> messages) {

    public boolean isValid() {
        return messages.stream().noneMatch(m -> m.severity() == ValidationSeverity.ERROR);
    }
}
