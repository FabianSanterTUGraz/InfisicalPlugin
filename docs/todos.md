# TODOs

<!-- Projekt-TODOs hier pflegen — verwaltbar mit /todo -->

## Inbox

- [x] #18 Browser-basierten Login-Flow für die Sidebar implementieren: bei Erstanmeldung Weiterleitung zur Infisical-Webanmeldung, Nutzer gibt Credentials einmalig im Browser ein, ein lokaler Callback-Listener (`localhost:8010`) wird temporär vom Plugin geöffnet und empfängt Access-/Refresh-Token, Speicherung über die IntelliJ PasswordSafe-API (damit erneutes Einloggen möglichst entfällt)

## Grundgerüst

- [x] #2 Package/Group von `com.example` auf echten Namespace umstellen (`build.gradle.kts` group, Kotlin-Package-Struktur, plugin.xml `id`)
- [x] #3 `plugin.xml` mit echten Metadaten füllen (Name, Vendor, Beschreibung, since-build/until-build)

## Infisical-Integration

- [x] #4 Authentifizierungsmethode festlegen (Machine Identity/Universal Auth vs. Service Token) und Auth-Flow implementieren
- [x] #5 HTTP-Client für die Infisical-REST-API implementieren (Secrets nach Projekt/Environment/Pfad abrufen)
- [x] #7 Zugangsdaten sicher speichern (IntelliJ PasswordSafe/CredentialStore statt Klartext in der Settings-XML)
- [x] #8 Geladene Secrets in Run/Debug-Konfigurationen injizieren (z.B. als Umgebungsvariablen vor Programmstart)
- [x] #11 Fehlerbehandlung für ungültiges Token, Netzwerkfehler, abgelaufene Auth (Notifications statt Absturz)
- [x] #19 Plugin-Verifier-Ergebnis (`./gradlew verifyPlugin`) prüfen und alle gemeldeten Kompatibilitäts-/API-Probleme beheben
- [ ] #24 Plugin-Name in `plugin.xml` enthält laut Plugin-Verifier das Wort "IntelliJ" (Marketplace-Namenskonvention) — umbenennen oder Verifier-Check bewusst muten
- [x] #25 Machine-spezifische Secrets (per lokalem Pfad-Override erkannt) zusätzlich serverseitig in Infisical mit Tag `specificpaths` markieren, damit Teammitglieder sie in der Web-App finden und einen Personal Override setzen können (fail-open bei Fehlern)

## Tests

- [x] #14 Plugin-Tests mit dem IntelliJ Platform Test Framework (Sanity-Test, Settings-UI-Test)
- [x] #20 Manuellen Rauchtest im Sandbox-IDE durchführen: Login-Flow, Secret-Abruf, Run-Config-Injection sowie Fehlerfälle (ungültiges Token, kein Netzwerk, abgelaufene Auth)

## CI/CD & Release

- [x] #15 `.gitlab-ci.yml` für Build/Test/Plugin-Verifier einrichten (`/gitlab-ci new`)
- [ ] #21 CHANGELOG.md für das Release befüllen (aktuell nur leerer `## [Unreleased]`-Abschnitt)
- [x] #22 `development` in `main` mergen und sicherstellen, dass die GitLab-CI-Pipeline (test, verifyPlugin) grün durchläuft, bevor getaggt wird
- [ ] #23 Release-Tag setzen (z.B. `v1.0.8`), um `buildPlugin`/`publishPlugin` auszulösen (`/version`)

## Dokumentation

- [x] #17 README.md mit echter Projektbeschreibung, Setup- und Nutzungsanleitung füllen
