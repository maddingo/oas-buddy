package no.maddin.oasbuddy.desktop;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * swagger-parser logs through slf4j. With no provider on the classpath slf4j falls back to a no-op
 * logger, so the parser's own explanation of why a document would not parse is discarded — and it
 * announces that on stderr the first time the validator runs.
 *
 * <p>Picking the provider belongs to the application, not to oas-buddy-core: a library that picks
 * one forces it on every consumer. The desktop module therefore routes slf4j into
 * {@code java.util.logging}, which is where the app's own {@code System.getLogger} calls already go,
 * so both end up in one place instead of two parallel logging systems.
 */
class LoggingProviderTest {

    @Test
    void slf4jHasAProviderSoLibraryDiagnosticsAreNotDiscarded() {
        assertFalse(LoggerFactory.getILoggerFactory() instanceof NOPLoggerFactory,
                "no slf4j provider on the classpath: swagger-parser's diagnostics go nowhere");
    }

    @Test
    void slf4jAndSystemLoggerOutputLandInTheSamePlace() {
        Logger root = LogManager.getLogManager().getLogger("");
        List<String> captured = new ArrayList<>();
        Handler collector = new Handler() {
            @Override
            public void publish(LogRecord record) {
                captured.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        collector.setLevel(Level.ALL);

        Level originalLevel = root.getLevel();
        root.addHandler(collector);
        root.setLevel(Level.ALL);
        try {
            LoggerFactory.getLogger("routing.through.slf4j").error("from slf4j");
            System.getLogger("routing.through.platform").log(System.Logger.Level.ERROR, "from System.Logger");
        } finally {
            root.removeHandler(collector);
            root.setLevel(originalLevel);
        }

        assertTrue(captured.contains("from slf4j"),
                "slf4j output never reached java.util.logging: " + captured);
        assertTrue(captured.contains("from System.Logger"),
                "System.Logger output never reached java.util.logging: " + captured);
    }
}
