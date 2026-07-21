# TODOs

<!-- Projekt-TODOs hier pflegen — verwaltbar mit /todo -->

## Inbox

- [ ] #18 Browser-basierten Login-Flow für die Sidebar implementieren: bei Erstanmeldung Weiterleitung zur Infisical-Webanmeldung, Nutzer gibt Credentials einmalig im Browser ein, ein lokaler Callback-Listener (`localhost:8010`) wird temporär vom Plugin geöffnet und empfängt Access-/Refresh-Token, Speicherung über die IntelliJ PasswordSafe-API (damit erneutes Einloggen möglichst entfällt)

## Grundgerüst

- [x] #2 Package/Group von `com.example` auf echten Namespace umstellen (`build.gradle.kts` group, Kotlin-Package-Struktur, plugin.xml `id`)
- [x ] #3 `plugin.xml` mit echten Metadaten füllen (Name, Vendor, Beschreibung, since-build/until-build)

## Infisical-Integration

- [x ] #4 Authentifizierungsmethode festlegen (Machine Identity/Universal Auth vs. Service Token) und Auth-Flow implementieren → [spec](specs/4-2026-07-14-authentifizierungsmethode-festlegen.md)
- [ x] #5 HTTP-Client für die Infisical-REST-API implementieren (Secrets nach Projekt/Environment/Pfad abrufen) → [spec](specs/5-2026-07-14-http-client-fuer-infisical-rest-api-implementieren.md) → [erkenntnis](../erkenntnisse/2026-07-14-infisical-v4-api-endpunkt.md)
- [ ] #6 Settings-Configurable anlegen: Infisical-Server-URL, Projekt-ID, Environment, Auth-Credentials konfigurierbar machen
- [ ] #7 Zugangsdaten sicher speichern (IntelliJ PasswordSafe/CredentialStore statt Klartext in der Settings-XML)
- [ ] #8 Geladene Secrets in Run/Debug-Konfigurationen injizieren (z.B. als Umgebungsvariablen vor Programmstart)
- [ ] #9 Tool-Window/Panel zur Anzeige der geladenen Secrets (mit Maskierung/Redaction)
- [ ] #10 Caching- und Refresh-Strategie für geladene Secrets (TTL, manuelle Refresh-Action)
- [ ] #11 Fehlerbehandlung für ungültiges Token, Netzwerkfehler, abgelaufene Auth (Notifications statt Absturz)

## Sicherheit

- [ ] #12 Sicherstellen, dass Secrets nie geloggt oder in Klartext auf Platte persistiert werden (Log-Filter, Redaction-Check)

## Tests

- [ ] #13 Unit-Tests für den Infisical-API-Client (Mock-Server, z.B. WireMock)
- [ ] #14 Plugin-Tests mit dem IntelliJ Platform Test Framework (Sanity-Test, Settings-UI-Test)

## CI/CD & Release

- [ ] #15 `.gitlab-ci.yml` für Build/Test/Plugin-Verifier einrichten (`/gitlab-ci new`)
- [ ] #16 Plugin-Signing und Marketplace-Publish vorbereiten (Zertifikate, Tokens, CHANGELOG-Pflege)

## Dokumentation

- [ ] #17 README.md mit echter Projektbeschreibung, Setup- und Nutzungsanleitung füllen