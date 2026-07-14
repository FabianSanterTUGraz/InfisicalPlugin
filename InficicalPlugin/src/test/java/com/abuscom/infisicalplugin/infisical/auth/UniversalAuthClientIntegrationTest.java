package com.abuscom.infisicalplugin.infisical.auth;

import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnabledIfEnvironmentVariable(named = "INFISICAL_TEST_CLIENT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "INFISICAL_TEST_CLIENT_SECRET", matches = ".+")
class UniversalAuthClientIntegrationTest {

    @Test
    void login_succeeds_againstRealApi() throws InfisicalHttpException {
        String clientId = System.getenv("INFISICAL_TEST_CLIENT_ID");
        String clientSecret = System.getenv("INFISICAL_TEST_CLIENT_SECRET");

        InfisicalHttpClient httpClient = new InfisicalHttpClient(InfisicalHttpClient.DEFAULT_BASE_URL);
        UniversalAuthClient authClient = new UniversalAuthClient(httpClient);

        AccessToken token = authClient.login(clientId, clientSecret);

        assertNotNull(token.value());
        assertFalse(token.value().isBlank());
        assertFalse(token.isExpired());
    }
}
