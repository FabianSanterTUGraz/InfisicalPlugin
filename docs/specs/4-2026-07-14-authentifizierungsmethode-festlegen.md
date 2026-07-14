# Spec: Authentifizierungsmethode festlegen und Auth-Flow implementieren

**TL;DR:** Nach Umsetzung existiert ein schlanker, wiederverwendbarer HTTP-Wrapper plus ein
`UniversalAuthClient`, der sich per Machine-Identity-Credentials (Client ID/Secret) gegen die
Infisical-API einloggt und ein `AccessToken` mit Ablaufzeitpunkt liefert — belegt durch
Unit-Tests (JDK-Mock-Server) und einen optionalen Integrationstest gegen die echte Infisical-Cloud-API.

**TODO-Referenz:** docs/todos.md → #4

## Kontext

Der Codebase ist aktuell ein leeres Gradle/Java-Grundgerüst — es existiert kein Java-Code, keine
HTTP- oder JSON-Dependency, nur `plugin.xml` und die Build-Konfiguration.

Entscheidung zur Methode (bereits im Vorgespräch getroffen): **Machine Identity / Universal Auth**
statt Service Token, da Service Tokens von Infisical als Legacy geführt werden und Universal Auth
kurzlebige Access-Tokens statt eines Dauer-Secrets verwendet.

Relevante Dateien:

- `InficicalPlugin/build.gradle.kts` — aktuell nur `junit` (JUnit 4) und `com.intellij.modules.json` als Dependency
- `InficicalPlugin/gradle/libs.versions.toml` — Version-Catalog, aktuell nur JUnit 4
- `InficicalPlugin/gradle.properties` — Group `com.abuscom.infisicalplugin`
- `InficicalPlugin/.infisical.json` — enthält eine echte `workspaceId` (`6f0488c9-3430-4e67-b093-3ab2c01cf026`) aus lokaler Infisical-CLI-Nutzung; relevant für den optionalen Integrationstest, aber keine Secrets-Fetch-Logik ist Teil dieser Spec

Getroffene Design-Entscheidungen (aus der Grilling-Session):

- **HTTP:** `java.net.http.HttpClient` (JDK, keine neue Dependency)
- **JSON:** Gson (von der IntelliJ Platform gebündelt, keine neue Dependency)
- **Fehlerverhalten:** eigene Checked Exception `InfisicalHttpException` (Status-Code + Body) bei Non-2xx und Netzwerkfehlern
- **API-Form:** eine generische `send(method, path, headers, body)`-Methode (deckt späteres GET für #5 ab, ohne Signatur-Änderung)
- **Basis-URL:** Konstruktor-Parameter mit `DEFAULT_BASE_URL = "https://app.infisical.com"` als Default (Self-Hosted-Support kommt über #6)
- **Threading:** synchron/blockierend; der Aufrufer ist für Thread-Wechsel (nicht auf dem EDT) verantwortlich
- **Token-Handling:** zustandsloser `login()`, der ein `AccessToken(value, expiresAt)` zurückgibt — kein Auto-Refresh (das gehört zu #10, nicht zu #4)
- **Tests:** Unit-Tests mit JDK-eigenem `com.sun.net.httpserver.HttpServer` als Mock (kein WireMock, das bleibt #13 für die volle API-Client-Abdeckung); zusätzlich ein env-var-gated Integrationstest gegen die echte Infisical-Cloud-API
- **JUnit:** Wechsel von JUnit 4 auf JUnit 5 (Jupiter), da `@EnabledIfEnvironmentVariable` für den Integrationstest benötigt wird

## Implementierungsschritte

- [ ] `InficicalPlugin/gradle/libs.versions.toml`: JUnit-4-Eintrag durch JUnit 5 (Jupiter) ersetzen (`junit-jupiter`, Version z.B. `5.10.2`)
- [ ] `InficicalPlugin/build.gradle.kts`: `testImplementation(libs.junit.jupiter)` statt `libs.junit`, zusätzlich `testRuntimeOnly("org.junit.platform:junit-platform-launcher")`
- [ ] Paket `com.abuscom.infisicalplugin.infisical.http` anlegen unter `InficicalPlugin/src/main/java/...`:
  - [ ] `HttpApiResponse` (Record: `int statusCode`, `Map<String,String> headers`, `String body`)
  - [ ] `InfisicalHttpException` (Checked Exception: `int statusCode`, `String responseBody`, Konstruktor auch für reine Netzwerkfehler ohne Status-Code)
  - [ ] `InfisicalHttpClient` (Konstruktor `InfisicalHttpClient(String baseUrl)`, Konstante `DEFAULT_BASE_URL`, Methode `HttpApiResponse send(String method, String path, Map<String,String> headers, String body) throws InfisicalHttpException`; intern `java.net.http.HttpClient` mit Connect-Timeout 10s und Request-Timeout 30s; wirft `InfisicalHttpException` bei Statuscode ≥ 300 sowie bei `IOException`/`InterruptedException`)
- [ ] Paket `com.abuscom.infisicalplugin.infisical.auth` anlegen:
  - [ ] `AccessToken` (Record: `String value`, `Instant expiresAt`, Methode `boolean isExpired()`)
  - [ ] Package-private Gson-DTOs `UniversalAuthLoginRequest` (`clientId`, `clientSecret`) und `UniversalAuthLoginResponse` (`accessToken`, `expiresIn` als Sekunden, `tokenType`) — **Feldnamen vor Implementierung gegen die aktuelle Infisical-API-Doku verifizieren** (siehe Offene Fragen)
  - [ ] `UniversalAuthClient` (Konstruktor nimmt `InfisicalHttpClient`; Methode `AccessToken login(String clientId, String clientSecret) throws InfisicalHttpException` → POST `/api/v1/auth/universal-auth/login`, Body via Gson serialisiert, Response via Gson deserialisiert, `expiresAt` = `Instant.now().plusSeconds(expiresIn)`)
- [ ] Unit-Test `InfisicalHttpClientTest` unter `InficicalPlugin/src/test/java/.../infisical/http/`: startet lokalen `com.sun.net.httpserver.HttpServer`, prüft Erfolgsfall (200 → `HttpApiResponse` korrekt befüllt) und Fehlerfall (z.B. 401 → `InfisicalHttpException` mit korrektem Status-Code)
- [ ] Unit-Test `UniversalAuthClientTest` unter `.../infisical/auth/`: nutzt denselben Mock-Server-Ansatz, um den Login-Request/Response-Zyklus zu prüfen (korrektes Body-Format gesendet, `AccessToken` korrekt aus Response befüllt)
- [ ] Integrationstest `UniversalAuthClientIntegrationTest` unter `.../infisical/auth/`: liest `INFISICAL_TEST_CLIENT_ID`/`INFISICAL_TEST_CLIENT_SECRET` aus der Umgebung, per `@EnabledIfEnvironmentVariable` nur aktiv wenn gesetzt; ruft echten Login gegen `DEFAULT_BASE_URL` auf und prüft dass ein nicht-leeres `AccessToken` zurückkommt
- [ ] `./gradlew build` ausführen und sicherstellen, dass alles kompiliert und die (nicht-gategen) Tests grün sind; falls Gson beim Kompilieren nicht auf dem Klassenpfad gefunden wird, `compileOnly("com.google.code.gson:gson:<von Platform genutzte Version>")` in `build.gradle.kts` ergänzen

## Akzeptanzkriterien

- [ ] `InfisicalHttpClient.send(...)` sendet Requests korrekt und wirft `InfisicalHttpException` bei Non-2xx-Antworten sowie bei Netzwerkfehlern
- [ ] `UniversalAuthClient.login(clientId, clientSecret)` liefert bei erfolgreichem Login ein `AccessToken` mit korrekt berechnetem `expiresAt`
- [ ] Unit-Tests (`InfisicalHttpClientTest`, `UniversalAuthClientTest`) laufen ohne externe Abhängigkeiten grün durch (`./gradlew test`)
- [ ] Der Integrationstest ist standardmäßig deaktiviert (kein Fail in CI ohne gesetzte Env-Vars) und läuft erfolgreich, wenn `INFISICAL_TEST_CLIENT_ID`/`INFISICAL_TEST_CLIENT_SECRET` lokal gesetzt sind
- [ ] `./gradlew build` läuft fehlerfrei mit JUnit 5 durch

## Out of Scope

- Secrets-Abruf über die Infisical-API (→ #5)
- Settings-UI für Server-URL/Projekt/Environment/Credentials (→ #6)
- Sichere Speicherung der Client ID/Secret (PasswordSafe/CredentialStore) (→ #7)
- Automatisches Token-Refresh/Caching bei Ablauf (→ #10)
- User-facing Fehlerbehandlung/Notifications bei ungültigen Credentials oder Netzwerkfehlern (→ #11)
- Umfassende WireMock-basierte Testabdeckung des gesamten API-Clients (→ #13)

## Offene Fragen

- Die exakten Feldnamen des Universal-Auth-Login-Endpoints (`clientId`/`clientSecret` im Request,
  `accessToken`/`expiresIn`/`tokenType` in der Response) sind aus bestehendem Wissen übernommen,
  aber nicht live gegen die aktuelle Infisical-API-Dokumentation verifiziert — vor der
  Implementierung der DTOs gegen `https://infisical.com/docs/api-reference` prüfen.
- Ob Gson ohne explizite Gradle-Dependency automatisch über den von der IntelliJ-Platform-Gradle-Plugin
  bereitgestellten Klassenpfad auflösbar ist, ist nicht verifiziert — falls nicht, `compileOnly`-Dependency
  gemäß letztem Implementierungsschritt ergänzen.