package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.ApiResponse;
import no.maddin.oasbuddy.core.model.Callback;
import no.maddin.oasbuddy.core.model.Example;
import no.maddin.oasbuddy.core.model.Header;
import no.maddin.oasbuddy.core.model.Link;
import no.maddin.oasbuddy.core.model.OperationIds;
import no.maddin.oasbuddy.core.model.Parameter;

import java.util.List;

/**
 * What may be referred to instead of defined inline: the responses, parameters, examples, headers, links and callbacks
 * declared under {@code components}. One interface for every kind, rather than one per kind, so
 * {@link OperationPane} does not grow a parameter each time another kind becomes referable; an
 * interface rather than the document for the same reason as {@link SecuritySchemeCatalog} — the
 * pane keeps knowing only about its own operation.
 */
public interface ComponentCatalog {

    /** The declared response names, in document order. */
    List<String> responseNames();

    /** The declared response, or {@code null} if there is none by that name. */
    ApiResponse response(String name);

    /** The declared parameter keys, in document order. */
    List<String> parameterNames();

    /** The declared parameter, or {@code null} if there is none under that key. */
    Parameter parameter(String name);

    /** The declared example names, in document order. */
    List<String> exampleNames();

    /** The declared example, or {@code null} if there is none by that name. */
    Example example(String name);

    /** The declared header names, in document order. */
    List<String> headerNames();

    /** The declared header, or {@code null} if there is none by that name. */
    Header header(String name);

    /** The declared link names, in document order. */
    List<String> linkNames();

    /** The declared link, or {@code null} if there is none by that name. */
    Link link(String name);

    /** The operation ids the document's paths declare: what a link may target. */
    List<String> operationIds();

    /** The declared callback names, in document order. */
    List<String> callbackNames();

    /** The declared callback, or {@code null} if there is none by that name. */
    Callback callback(String name);

    /** Reads straight off the document, so the catalog is never stale. */
    static ComponentCatalog of(OasDocument document) {
        return new ComponentCatalog() {
            @Override
            public List<String> responseNames() {
                return document.getComponents().getResponses().names();
            }

            @Override
            public ApiResponse response(String name) {
                return document.getComponents().getResponses().getResponse(name);
            }

            @Override
            public List<String> parameterNames() {
                return document.getComponents().getParameters().names();
            }

            @Override
            public Parameter parameter(String name) {
                return document.getComponents().getParameters().getParameter(name);
            }

            @Override
            public List<String> exampleNames() {
                return document.getComponents().getExamples().names();
            }

            @Override
            public List<String> headerNames() {
                return document.getComponents().getHeaders().names();
            }

            @Override
            public Header header(String name) {
                return document.getComponents().getHeaders().getHeader(name);
            }

            @Override
            public List<String> callbackNames() {
                return document.getComponents().getCallbacks().names();
            }

            @Override
            public Callback callback(String name) {
                return document.getComponents().getCallbacks().getCallback(name);
            }

            @Override
            public List<String> linkNames() {
                return document.getComponents().getLinks().names();
            }

            @Override
            public Link link(String name) {
                return document.getComponents().getLinks().getLink(name);
            }

            @Override
            public List<String> operationIds() {
                return OperationIds.all(document);
            }

            @Override
            public Example example(String name) {
                return document.getComponents().getExamples().getExample(name);
            }
        };
    }
}
