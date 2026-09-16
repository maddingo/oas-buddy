package no.maddin.oasbuddy.desktop;

import javafx.application.Application;

/**
 * Entry point used for plain classpath launches (e.g. running from an IDE). The JVM refuses to
 * launch a main class that itself extends {@link Application} without an explicit module-path,
 * even when the JavaFX jars are on the classpath, so this indirection is required.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }
}
