package com.abuscom.infisicalplugin.infisical.login;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

public class LoginTests {

    private static final String CALLBACK_URL = "http://127.0.0.1:" + 8010;

    private final TokenManager tokenManager = TokenManager.getInstance();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private LoginCallBackServer server;

    @AfterEach
    void tearDown() throws ReflectiveOperationException {
        // LoginCallBackServer only stops itself internally after a successful POST -
        // there is no public method to stop it from the outside, so tests that don't
        // reach that success path (OPTIONS, malformed body) would otherwise leave the
        // fixed port 8010 bound for the next test. Reflection is the least invasive way
        // to clean up without changing production code just for testability.
        if (server != null) {
            Field field = LoginCallBackServer.class.getDeclaredField("server");
            field.setAccessible(true);
            HttpServer httpServer = (HttpServer) field.get(server);
            if (httpServer != null) {
                httpServer.stop(0);
            }
            server = null;
        }
    }

    private void startFreshServer() throws IOException {
        server = new LoginCallBackServer(tokenManager);
        server.startServer();
    }

    @Test
    void get_two_times_token() {
        TokenManager first = TokenManager.getInstance();
        TokenManager second = TokenManager.getInstance();

        assertSame(first, second);
    }

    @Test
    void optionsRequest_returnsCorsHeaders() throws IOException, InterruptedException {
        startFreshServer();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CALLBACK_URL))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        assertEquals(200, response.statusCode());
        assertEquals("*", response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        assertEquals("GET, POST, OPTIONS", response.headers().firstValue("Access-Control-Allow-Methods").orElse(null));
        assertEquals("Content-Type", response.headers().firstValue("Access-Control-Allow-Headers").orElse(null));
    }

    @Test
    void postWithMalformedJson_abortsConnectionInsteadOfCrashingServerProcess() throws IOException {
        startFreshServer();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CALLBACK_URL))
                .POST(HttpRequest.BodyPublishers.ofString("not-json"))
                .build();

        // JsonParser.parseString throws before any token/keychain code runs, so this is
        // independent of the PasswordSafe blocker below. handle() has no try/catch around
        // the parsing, so the JDK's HttpServer aborts the connection instead of sending a
        // clean error response - the client sees an IOException, not a 4xx/5xx status.
        assertThrows(IOException.class,
                () -> httpClient.send(request, HttpResponse.BodyHandlers.discarding()));
    }

    // The following are blocked, not missing - do not add @Test back without first solving
    // the root cause:
    //
    // TokenManager.setTokenInKeypass/getTokenFromKeypass/clearKeypass all call
    // PasswordSafe.getInstance(), which requires a running IntelliJ Platform Application.
    // Plain JUnit 5 tests in this module do not start one, even though the Gradle test task
    // runs inside `.intellijPlatform/sandbox/...` (that only wires up classpath/logging, not
    // a real Application). Confirmed by running this suite: every call into those three
    // TokenManager methods throws NullPointerException.
    //
    // build.gradle.kts already declares `testFramework(TestFrameworkType.Platform)`, but that
    // alone doesn't pull in the JUnit 5 fixture API (`com.intellij.testFramework.junit5.*`,
    // e.g. @TestApplication) - that class exists in the IDE's bundled lib/testFramework.jar
    // but was not resolvable on this project's test compile classpath when tried.
    //
    // Blocked test cases, to write once a real Application is available in tests:
    // - removedListener_isNotNotified
    // - activeListener_receivesToken
    // - multipleListeners_allReceiveToken
    // - clearKeypass_notifiesWithNull
    // - postWithValidJson_storesTokenAndRespondsSuccessfully
    // - serverStopsAfterSuccessfulPost_secondRequestFails (regression test for the port-leak
    //   fix in commit 2713426)

    // buildLoginUrl() in LoginUser is private and static, so it isn't reachable from a test
    // in this package either. Testing it would require making it package-private or
    // extracting it into its own class - worth discussing before changing production code
    // just for testability.
}
