package no.maddin.oasbuddy.desktop;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * What the application knows about itself: name, version, license, links and the people credited
 * for it.
 *
 * <p>All of it is read from {@code version.properties}, which Maven fills in from the POM during
 * the resources phase — see the {@code <resources>} block in the desktop module's POM. Nothing here
 * is a Java constant, deliberately: the versioning contract in {@code CLAUDE.md} keeps the real
 * version out of git entirely (a release build passes {@code -Drevision=X.Y.Z}), so a constant
 * would either be a lie or need a version-bump commit. The same argument covers the url and the
 * license, which the POM already has to declare.
 *
 * <p>A build that never ran the filter leaves the values as literal {@code ${...}} expressions.
 * That happens for real — an IDE running straight from its own output directory, or a test on a
 * bare classpath — so an unsubstituted value counts as absent rather than being shown to the user:
 * the version falls back to {@link #UNKNOWN_VERSION} and every other field comes back empty, which
 * the About dialog renders by leaving that line out.
 */
public record AppInfo(String name, String version, String description, String url, String readmeUrl,
                      String contributorsUrl, String copyright, String licenseName, String licenseUrl,
                      List<Person> developers) {

    static final String RESOURCE = "/no/maddin/oasbuddy/desktop/version.properties";

    /** Shown instead of a version when the build metadata never made it onto the classpath. */
    public static final String UNKNOWN_VERSION = "development build";

    /**
     * How many {@code developer.N.*} slots {@code version.properties} declares.
     *
     * <p>Resource filtering has no loop construct, so a list of arbitrary length is spelled out as
     * a fixed number of slots. Maven leaves an out-of-range index unsubstituted rather than failing
     * the build, so reading stops at the first slot the POM did not fill and the cap only limits
     * how many developers can be credited, never whether the file is valid.
     */
    private static final int DEVELOPER_SLOTS = 4;

    /** Someone credited in the About dialog; {@code url} may be empty. */
    public record Person(String name, String url) {
    }

    /** Reads the build metadata off the classpath, falling back rather than failing. */
    public static AppInfo load() {
        try (InputStream in = AppInfo.class.getResourceAsStream(RESOURCE)) {
            return read(in);
        } catch (IOException e) {
            return read(null);
        }
    }

    /** Package-private so the tests can supply an absent, unfiltered or partial resource. */
    static AppInfo read(InputStream in) {
        Properties properties = parse(in);

        String year = value(properties, "inceptionYear");
        String organization = value(properties, "organization");
        String version = value(properties, "version");

        return new AppInfo(
                value(properties, "name"),
                version.isEmpty() ? UNKNOWN_VERSION : version,
                value(properties, "description"),
                value(properties, "url"),
                value(properties, "readmeUrl"),
                value(properties, "contributorsUrl"),
                year.isEmpty() || organization.isEmpty() ? "" : "Copyright © " + year + " " + organization,
                value(properties, "licenseName"),
                value(properties, "licenseUrl"),
                developers(properties));
    }

    private static List<Person> developers(Properties properties) {
        List<Person> developers = new ArrayList<>();
        for (int slot = 0; slot < DEVELOPER_SLOTS; slot++) {
            String name = value(properties, "developer." + slot + ".name");
            if (name.isEmpty()) {
                break;
            }
            developers.add(new Person(name, value(properties, "developer." + slot + ".url")));
        }
        return List.copyOf(developers);
    }

    private static Properties parse(InputStream in) {
        Properties properties = new Properties();
        if (in == null) {
            return properties;
        }
        // Properties.load(InputStream) decodes ISO-8859-1; a person's name is not necessarily in it.
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            // An unreadable resource is no different from a missing one: the editor still starts.
            System.getLogger(AppInfo.class.getName())
                    .log(System.Logger.Level.WARNING, "could not read " + RESOURCE, e);
        }
        return properties;
    }

    private static String value(Properties properties, String key) {
        String value = properties.getProperty(key, "").trim();
        return value.startsWith("${") ? "" : value;
    }
}
