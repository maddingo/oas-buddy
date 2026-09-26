package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import no.maddin.oasbuddy.core.model.PathItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Removing paths and operations, with the confirmation stubbed out. Nothing can $ref a path, so
 * unlike a schema removal the only thing worth warning about is what the removal takes with it.
 */
class PathRemovalTest {

    @Test
    void removesThePathAndNotifiesWhenConfirmed() {
        OasDocument document = petstore();
        AtomicBoolean notified = new AtomicBoolean();

        PathRemoval.removePath(document, "/pets", (question, details) -> true, () -> notified.set(true));

        assertEquals(List.of("/pets/{petId}"), document.getPaths().pathNames());
        assertTrue(notified.get());
    }

    @Test
    void keepsThePathAndStaysQuietWhenDeclined() {
        OasDocument document = petstore();
        AtomicBoolean notified = new AtomicBoolean();

        PathRemoval.removePath(document, "/pets", (question, details) -> false, () -> notified.set(true));

        assertTrue(document.getPaths().pathNames().contains("/pets"));
        assertFalse(notified.get());
    }

    @Test
    void warnsThatRemovingAPathTakesItsOperationsWithIt() {
        OasDocument document = petstore();
        List<String> asked = new ArrayList<>();

        PathRemoval.removePath(document, "/pets", (question, details) -> {
            asked.add(question + "\n" + details);
            return false;
        }, () -> { });

        assertEquals(List.of("""
                Remove path "/pets"?
                It also removes these operations:
                  GET — listPets
                  POST — createPet"""), asked);
    }

    @Test
    void saysSoWhenThePathHasNoOperations() {
        OasDocument document = petstore();
        document.getPaths().addPath("/empty");
        List<String> asked = new ArrayList<>();

        PathRemoval.removePath(document, "/empty", (question, details) -> {
            asked.add(details);
            return false;
        }, () -> { });

        assertEquals(List.of("It has no operations."), asked);
    }

    /** A hand-edited {@code /odd: ~} is still a path the user must be able to get rid of. */
    @Test
    void aPathThatIsNotAnObjectCanStillBeRemoved() {
        OasDocument document = petstore();
        ((com.fasterxml.jackson.databind.node.ObjectNode) document.getRoot().get("paths")).putNull("/odd");
        List<String> asked = new ArrayList<>();

        PathRemoval.removePath(document, "/odd", (question, details) -> {
            asked.add(question + " | " + details);
            return true;
        }, () -> { });

        assertEquals(List.of("Remove path \"/odd\"? | It has no operations."), asked);
        assertFalse(document.getPaths().pathNames().contains("/odd"));
    }

    @Test
    void removesOnlyTheChosenOperation() {
        OasDocument document = petstore();

        PathRemoval.removeOperation(document, "/pets", HttpMethod.GET, (question, details) -> true, () -> { });

        PathItem pets = document.getPaths().getPathItem("/pets");
        assertEquals(List.of(HttpMethod.POST), List.copyOf(pets.getOperations().keySet()));
        assertTrue(document.getPaths().pathNames().contains("/pets"), "the path itself stays");
    }

    @Test
    void keepsTheOperationWhenDeclined() {
        OasDocument document = petstore();

        PathRemoval.removeOperation(document, "/pets", HttpMethod.GET, (question, details) -> false, () -> { });

        assertTrue(document.getPaths().getPathItem("/pets").getOperations().containsKey(HttpMethod.GET));
    }

    @Test
    void namesTheOperationItIsAboutToRemove() {
        OasDocument document = petstore();
        List<String> asked = new ArrayList<>();

        PathRemoval.removeOperation(document, "/pets", HttpMethod.GET, (question, details) -> {
            asked.add(question + "\n" + details);
            return false;
        }, () -> { });

        assertEquals(List.of("""
                Remove GET /pets?
                listPets — List all pets"""), asked);
    }

    @Test
    void confirmsAnUnnamedOperationWithoutDetails() {
        OasDocument document = petstore();
        document.getPaths().addPath("/bare").addOperation(HttpMethod.DELETE);
        List<String> asked = new ArrayList<>();

        PathRemoval.removeOperation(document, "/bare", HttpMethod.DELETE, (question, details) -> {
            asked.add(question + "|" + details);
            return false;
        }, () -> { });

        assertEquals(List.of("Remove DELETE /bare?|"), asked);
    }

    private static OasDocument petstore() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        PathItem pets = document.getPaths().addPath("/pets");
        var list = pets.addOperation(HttpMethod.GET);
        list.setOperationId("listPets");
        list.setSummary("List all pets");
        pets.addOperation(HttpMethod.POST).setOperationId("createPet");
        document.getPaths().addPath("/pets/{petId}").addOperation(HttpMethod.GET);
        return document;
    }
}
