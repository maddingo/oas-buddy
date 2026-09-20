package no.maddin.oasbuddy.core.model;

/**
 * The URL an oauth2 flow can carry. Which of these a given flow actually defines is spec knowledge
 * and lives in {@link OAuthFlows#urlsOf(String)}.
 *
 * <p>Only the JSON field name belongs here; how it is labelled on screen is the editor's business.
 */
public enum OAuthFlowUrl {

    AUTHORIZATION_URL("authorizationUrl"),
    TOKEN_URL("tokenUrl"),
    REFRESH_URL("refreshUrl");

    private final String field;

    OAuthFlowUrl(String field) {
        this.field = field;
    }

    public String field() {
        return field;
    }
}
