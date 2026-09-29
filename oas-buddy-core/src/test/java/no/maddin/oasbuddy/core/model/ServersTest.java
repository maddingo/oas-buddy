package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.DocumentWriter;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServersTest {

    private final ObjectNode parent = JsonNodeFactory.instance.objectNode();

    @Test
    void readingNeverAddsTheKey() {
        Servers servers = new Servers(parent);

        assertTrue(servers.all().isEmpty());
        assertFalse(servers.isDeclared());
        assertFalse(parent.has("servers"));
    }

    @Test
    void anEmptyOverrideIsDistinguishableFromNoOverride() {
        Servers servers = new Servers(parent);
        servers.declare();

        assertTrue(servers.isDeclared());
        assertTrue(servers.all().isEmpty());

        Server added = servers.add("https://a");
        servers.remove(added);
        assertTrue(servers.isDeclared(), "removing the last entry keeps the override");

        servers.undeclare();
        assertFalse(parent.has("servers"));
    }

    @Test
    void removalIsByEntryNotByPosition() {
        Servers servers = new Servers(parent);
        servers.add("https://a");
        Server b = servers.add("https://b");
        servers.add("https://a");

        servers.remove(b);

        assertEquals(List.of("https://a", "https://a"), servers.all().stream().map(Server::getUrl).toList());
    }

    @Test
    void variablesAreLazyRenamedInPlaceAndTheKeyGoesWithTheLastOne() {
        Server server = new Servers(parent).add("https://{region}.example.com/{version}");
        ServerVariables variables = server.getVariables();
        assertTrue(variables.names().isEmpty());

        variables.add("region").setEnum(List.of("eu", "us"));
        variables.get("region").setDefault("eu");
        variables.add("version");
        assertEquals(List.of("region", "version"), variables.names());

        assertTrue(variables.rename("region", "zone"));
        assertEquals(List.of("zone", "version"), variables.names(), "renaming keeps the position");
        assertFalse(variables.rename("zone", "version"), "a collision is refused");

        variables.get("zone").setEnum(List.of());
        assertFalse(((ObjectNode) parent.get("servers").get(0).get("variables").get("zone")).has("enum"));

        variables.remove("zone");
        variables.remove("version");
        assertFalse(parent.get("servers").get(0).has("variables"));
    }

    @Test
    void aTemplateWithoutAVariableIsNoticed() {
        Server server = new Servers(parent).add("https://{region}.example.com/{version}/{region}");
        assertEquals(List.of("region", "version"), server.templateNames());

        server.getVariables().add("region");

        assertEquals(List.of("version"), server.undefinedVariables());
    }

    @Test
    void aVariableThatIsNotAnObjectIsListedButNotReadable() throws IOException {
        OasDocument document = DocumentReader.read(
                "{\"openapi\":\"3.0.3\",\"servers\":[{\"url\":\"https://{a}\",\"variables\":{\"a\":\"x\"}}]}",
                DocumentFormat.JSON);
        Server server = document.getServers().all().get(0);

        assertEquals(List.of("a"), server.getVariables().names());
        assertNull(server.getVariables().get("a"));
        assertTrue(server.undefinedVariables().isEmpty());
    }

    @Test
    void operationDeprecatedWritesOnlyTrue() {
        Operation operation = new Operation(parent);

        operation.setDeprecated(true);
        assertTrue(operation.isDeprecated());
        operation.setDeprecated(false);
        assertFalse(parent.has("deprecated"));
    }

    @Test
    void serversOnPathAndOperationRoundTripThroughYamlAndJson() throws IOException {
        for (DocumentFormat format : DocumentFormat.values()) {
            OasDocument document = OasDocument.newDocument(format);
            Server root = document.getServers().add("https://{env}.example.com");
            root.getVariables().add("env").setEnum(List.of("dev", "prod"));
            PathItem path = document.getPaths().addPath("/pets");
            path.getServers().add("https://pets.example.com");
            Operation get = path.addOperation(HttpMethod.GET);
            get.getServers().declare();
            get.setDeprecated(true);

            OasDocument reloaded = DocumentReader.read(DocumentWriter.write(document), format);

            assertEquals(document.getRoot(), reloaded.getRoot());
            assertTrue(reloaded.getPaths().getPathItem("/pets").getOperation(HttpMethod.GET)
                    .getServers().isDeclared());
        }
    }

    @Test
    void openingADocumentWithoutServersAddsNone() throws IOException {
        OasDocument document = DocumentReader.read("{\"openapi\":\"3.0.3\",\"paths\":{}}", DocumentFormat.JSON);

        document.getServers().all();

        assertFalse(document.getRoot().has("servers"));
    }
}
