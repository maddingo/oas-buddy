package no.maddin.oasbuddy.desktop;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AppInfo is the runtime end of the versioning contract in CLAUDE.md: every string the About dialog
 * shows is filtered into version.properties from the POM, so none of them is a Java constant that
 * can drift from what was actually released.
 */
class AppInfoTest {

    @Test
    void theVersionIsTheOneMavenBuiltWith() {
        // Surefire hands the test the reactor's own project.version. If resource filtering ever
        // stopped covering version.properties the resource would hold a literal "${project.version}"
        // and this would fail rather than quietly shipping a placeholder.
        assertEquals(System.getProperty("project.version"), AppInfo.load().version());
    }

    @Test
    void everythingTheAboutDialogShowsComesFromThePom() {
        AppInfo info = AppInfo.load();

        assertAll(
                () -> assertEquals("OAS Buddy", info.name()),
                () -> assertEquals("An OpenAPI Specification editor.", info.description()),
                () -> assertEquals("https://github.com/maddingo/oas-buddy", info.url()),
                () -> assertEquals("https://github.com/maddingo/oas-buddy#readme", info.readmeUrl()),
                () -> assertEquals("https://github.com/maddingo/oas-buddy/graphs/contributors",
                        info.contributorsUrl()),
                () -> assertEquals("Copyright © 2026 OAS Buddy contributors", info.copyright()),
                () -> assertEquals("Apache License, Version 2.0", info.licenseName()),
                () -> assertEquals("https://www.apache.org/licenses/LICENSE-2.0.txt", info.licenseUrl()),
                () -> assertEquals(
                        List.of(new AppInfo.Person("Martin Goldhahn", "https://github.com/maddingo")),
                        info.developers()));
    }

    @Test
    void aMissingResourceFallsBackToADevelopmentBuild() {
        AppInfo info = AppInfo.read(null);

        assertAll(
                () -> assertEquals("development build", info.version()),
                () -> assertEquals("", info.url()),
                () -> assertEquals(List.of(), info.developers()));
    }

    @Test
    void anUnfilteredResourceFallsBackInsteadOfShowingPlaceholders() {
        AppInfo info = read("""
                name=${project.parent.name}
                version=${project.version}
                url=${project.url}
                developer.0.name=${project.developers[0].name}
                """);

        assertAll(
                () -> assertEquals("development build", info.version()),
                () -> assertEquals("", info.name()),
                () -> assertEquals("", info.url()),
                () -> assertEquals(List.of(), info.developers()));
    }

    @Test
    void developersAreReadUntilTheFirstUnfilledSlot() {
        AppInfo info = read("""
                developer.0.name=Martin Goldhahn
                developer.0.url=https://github.com/maddingo
                developer.1.name=Someone Else
                developer.1.url=${project.developers[1].url}
                developer.2.name=${project.developers[2].name}
                developer.3.name=Never Reached
                """);

        assertEquals(
                List.of(new AppInfo.Person("Martin Goldhahn", "https://github.com/maddingo"),
                        new AppInfo.Person("Someone Else", "")),
                info.developers());
    }

    @Test
    void theCopyrightLineNeedsBothTheYearAndTheOrganisation() {
        assertAll(
                () -> assertEquals("Copyright \u00a9 2026 OAS Buddy contributors",
                        read("inceptionYear=2026\norganization=OAS Buddy contributors\n").copyright()),
                () -> assertEquals("",
                        read("inceptionYear=2026\norganization=${project.organization.name}\n").copyright()));
    }

    private static AppInfo read(String properties) {
        InputStream in = new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8));
        return AppInfo.read(in);
    }
}
