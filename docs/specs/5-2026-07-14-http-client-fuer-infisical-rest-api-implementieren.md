# Spec: HTTP-Client für die Infisical-REST-API implementieren

**TL;DR:** Nach Umsetzung existiert ein `SecretsClient`, der Secrets für ein gegebenes
Projekt/Environment/Pfad über den `GET /api/v4/secrets`-Endpoint abruft und als schlanke
`Secret(key, value)`-Liste zurückliefert — belegt durch Unit-Tests (JDK-Mock-Server) und einen
optionalen Integrationstest gegen die echte Infisical-Cloud-API.

**TODO-Referenz:** docs/todos.md → #5

## Kontext

TODO #4 ist bereits implementiert: `InfisicalHttpClient` (generischer `send(method, path,
headers, body)`-Wrapper um `java.net.http.HttpClient`, wirft `InfisicalHttpException` bei
Non-2xx/Netzwerkfehlern) und `UniversalAuthClient.login(clientId, clientSecret)` (liefert ein
`AccessToken(value, expiresAt)`).

Relevante Dateien:

- `InficicalPlugin/src/main/java/com/abuscom/infisicalplugin/infisical/http/InfisicalHttpClient.java` — bestehender HTTP-Wrapper, wird unverändert wiederverwendet
- `InficicalPlugin/src/main/java/com/abuscom/infisicalplugin/infisical/http/InfisicalHttpException.java` — bestehende Exception, wird unverändert wiederverwendet
- `InficicalPlugin/src/main/java/com/abuscom/infisicalplugin/infisical/auth/AccessToken.java` — liefert den Bearer-Token für die Requests dieser Spec
- `InficicalPlugin/src/main/java/com/abuscom/infisicalplugin/infisical/auth/UniversalAuthClient.java` — Vorbild für Gson-Nutzung und Client-Struktur
- `InficicalPlugin/src/test/java/.../infisical/http/InfisicalHttpClientTest.java` und `.../infisical/auth/UniversalAuthClientIntegrationTest.java` — Vorbild für Test-Muster (JDK-`HttpServer`-Mock bzw. env-var-gated Integrationstest)

Recherche zum aktuellen Infisical-API-Stand (infisical.com/docs/api-reference, Stand
2026-07-14): der frühere `/api/v3/secrets/raw`-Endpoint ist nicht mehr der empfohlene Weg —
aktuell ist `GET /api/v4/secrets` mit Query-Parametern `projectId`, `environment`, `secretPath`,
`recursive`, `includeImports`, `viewSecretValue` (Auth via `Authorization: Bearer <token>`). Die
Response hat die Form `{"secrets": [...], "imports": [...]}`; jedes Secret-Objekt enthält u.a.
`secretKey`, `secretValue`, `secretComment`, `secretValueHidden`, `tags`.

Getroffene Design-Entscheidungen (aus der Grilling-Session):

- **Rückgabetyp:** minimaler Record `Secret(String key, String value)` — kein Comment/Tags/Pfad,
  das kann bei Bedarf später ergänzt werden, ohne bestehende Konsumenten zu brechen
- **Methoden-Parameter:** `projectId`, `environment`, `secretPath` als Pflicht-Parameter;
  `recursive=false` und `includeImports=false` fest im Request (keine Methoden-Parameter)
- **Imports:** `includeImports=false` — importierte Secrets aus anderen Pfaden werden nicht
  abgerufen/gemerged; das `imports`-Feld der Response wird ignoriert
- **`viewSecretValue`:** fest auf `true` gesetzt (kein Parameter) — der Zweck des Clients ist,
  Werte abzurufen
- **Authentifizierung:** `AccessToken` wird pro Methodenaufruf übergeben (zustandsloser Client,
  analog zu `UniversalAuthClient.login(...)`), nicht im Konstruktor gespeichert — der Aufrufer
  bleibt verantwortlich für Token-Gültigkeit/Refresh (relevant für #10)
- **Hidden Values:** falls `secretValueHidden=true` und `secretValue` von der API leer/`null`
  geliefert wird, wird das Secret trotzdem mit diesem Wert übernommen — keine Filterung, kein
  Sonderfall im Code
- **Naming/Package:** `SecretsClient` in neuem Package
  `com.abuscom.infisicalplugin.infisical.secrets`, Methode `listSecrets(InfisicalHttpClient
  httpClient, AccessToken accessToken, String projectId, String environment, String
  secretPath)` (analog zur bestehenden Struktur `http/` und `auth/`)
- **Query-Encoding:** Query-Parameter werden einzeln mit `URLEncoder.encode(..., UTF_8)` kodiert
  und zu einem Query-String zusammengesetzt (die bestehende `InfisicalHttpClient.send`-Methode
  nimmt den fertigen `path` inkl. Query-String entgegen, baut selbst keine Query-Strings)
- **Tests:** gleiches Muster wie #4 — Unit-Tests mit JDK-`com.sun.net.httpserver.HttpServer` als
  Mock (Erfolgsfall mit mehreren Secrets, leere Liste, Secret mit `secretValueHidden=true`);
  zusätzlich ein env-var-gated Integrationstest gegen die echte Infisical-Cloud-API (nutzt
  bestehende `INFISICAL_TEST_CLIENT_ID`/`INFISICAL_TEST_CLIENT_SECRET` für den Login plus neue
  `INFISICAL_TEST_PROJECT_ID`/`INFISICAL_TEST_ENVIRONMENT` für den Secrets-Abruf; `secretPath`
  fest `"/"`)

## Implementierungsschritte

- [ ] Paket `com.abuscom.infisicalplugin.infisical.secrets` anlegen unter
  `InficicalPlugin/src/main/java/...`:
  - [ ] `Secret` (Record: `String key`, `String value`)
  - [ ] Package-private Gson-DTOs `SecretDto` (Felder `secretKey`, `secretValue` — restliche
    JSON-Felder der API werden von Gson automatisch ignoriert) und `ListSecretsResponse` (Feld
    `List<SecretDto> secrets` — `imports` wird nicht gemappt, da `includeImports=false`)
  - [ ] `SecretsClient` mit Konstante `LIST_SECRETS_PATH = "/api/v4/secrets"` und Methode
    `List<Secret> listSecrets(InfisicalHttpClient httpClient, AccessToken accessToken, String
    projectId, String environment, String secretPath) throws InfisicalHttpException`:
    - baut den Query-String aus `projectId`, `environment`, `secretPath` (je
      `URLEncoder.encode(..., StandardCharsets.UTF_8)`), fest ergänzt um `recursive=false`,
      `includeImports=false`, `viewSecretValue=true`
    - ruft `httpClient.send("GET", LIST_SECRETS_PATH + "?" + queryString, Map.of("Authorization",
      "Bearer " + accessToken.value()), null)` auf
    - deserialisiert die Response via Gson zu `ListSecretsResponse`, mapped jedes `SecretDto` zu
      `Secret(dto.secretKey(), dto.secretValue())`
    - liefert eine leere Liste falls `secrets` in der Response `null` oder leer ist
- [ ] Unit-Test `SecretsClientTest` unter `InficicalPlugin/src/test/java/.../infisical/secrets/`:
  startet lokalen `com.sun.net.httpserver.HttpServer` (analog zu `InfisicalHttpClientTest`),
  deckt ab:
  - [ ] Erfolgsfall mit mehreren Secrets in der Response → korrekt gemappte `List<Secret>`
  - [ ] leere `secrets`-Liste in der Response → leere `List<Secret>`, keine Exception
  - [ ] ein Secret mit `secretValueHidden: true` und `secretValue: null` in der Response → wird
    trotzdem mit `value = null` übernommen
  - [ ] Query-Parameter werden korrekt gesetzt (Mock-Handler prüft `exchange.getRequestURI()` auf
    `projectId`, `environment`, `secretPath`, `recursive=false`, `includeImports=false`,
    `viewSecretValue=true`) und `Authorization`-Header wird korrekt gesetzt
  - [ ] Non-2xx-Antwort (z.B. 403) → `InfisicalHttpException` wird propagiert (kein Sonderverhalten
    nötig, da `InfisicalHttpClient.send` das bereits wirft)
- [ ] Integrationstest `SecretsClientIntegrationTest` unter `.../infisical/secrets/`: liest
  `INFISICAL_TEST_CLIENT_ID`, `INFISICAL_TEST_CLIENT_SECRET`, `INFISICAL_TEST_PROJECT_ID`,
  `INFISICAL_TEST_ENVIRONMENT` aus der Umgebung, per `@EnabledIfEnvironmentVariable` nur aktiv
  wenn alle vier gesetzt sind; loggt sich per `UniversalAuthClient.login(...)` ein, ruft
  `SecretsClient.listSecrets(...)` mit `secretPath = "/"` auf und prüft, dass die zurückgegebene
  Liste kein Fehler wirft (Anzahl kann 0 sein, falls das Test-Projekt/Environment keine Secrets
  im Root-Pfad hat)
- [ ] `./gradlew build` ausführen und sicherstellen, dass alles kompiliert und die
  (nicht-gategen) Tests grün sind

## Akzeptanzkriterien

- [ ] `SecretsClient.listSecrets(httpClient, accessToken, projectId, environment, secretPath)`
  liefert bei Erfolg eine korrekt gemappte `List<Secret>` (key/value aus `secretKey`/`secretValue`)
- [ ] Der Request enthält `Authorization: Bearer <token>` und die Query-Parameter `projectId`,
  `environment`, `secretPath`, `recursive=false`, `includeImports=false`, `viewSecretValue=true`
- [ ] Eine leere `secrets`-Liste in der Response führt zu einer leeren `List<Secret>`, keiner
  Exception
- [ ] Non-2xx-Antworten führen zu `InfisicalHttpException` (propagiert aus `InfisicalHttpClient`)
- [ ] Unit-Tests (`SecretsClientTest`) laufen ohne externe Abhängigkeiten grün durch
  (`./gradlew test`)
- [ ] Der Integrationstest ist standardmäßig deaktiviert (kein Fail in CI ohne gesetzte Env-Vars)
  und läuft erfolgreich, wenn alle vier `INFISICAL_TEST_*`-Variablen lokal gesetzt sind
- [ ] `./gradlew build` läuft fehlerfrei durch

## Out of Scope

- Merging importierter Secrets (`includeImports=true` + `imports`-Array) — kann bei Bedarf später
  als Erweiterung nachgezogen werden
- Rekursiver Abruf über Unterpfade (`recursive=true`)
- Comment/Tags/Metadaten je Secret im Rückgabetyp
- Settings-UI für Projekt-ID/Environment (→ #6)
- Sichere Speicherung von Credentials (→ #7)
- Injection der Secrets in Run/Debug-Konfigurationen (→ #8)
- Tool-Window/Anzeige der Secrets (→ #9)
- Caching/Refresh-Strategie, automatisches Token-Refresh (→ #10)
- User-facing Fehlerbehandlung/Notifications (→ #11)
- Umfassende WireMock-basierte Testabdeckung des gesamten API-Clients (→ #13)
