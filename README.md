# InfisicalPlugin

IntelliJ-Platform-Plugin zur Integration von [Infisical](https://infisical.com/) (Secrets-Management)
in die IDE — Secrets werden über die Infisical-REST-API geladen und nutzbar gemacht.
Authentifizierung erfolgt über Machine Identity / Universal Auth.

## Setup

```bash
# Bauen (lädt Dependencies, kompiliert, führt Tests aus)
./gradlew build

# Nur Tests ausführen
./gradlew test

# Plugin in einer Sandbox-IDE starten
./gradlew runIde
```

## Stand

- **Fertig:** HTTP-Wrapper (`infisical.http`) und Universal-Auth-Login (`infisical.auth`) gegen
  die Infisical-API, inkl. Unit- und Integrationstests.
- **Offen:** Secrets-Abruf nach Projekt/Environment/Pfad, Settings-UI, sichere Speicherung der
  Credentials, Anzeige/Injection der Secrets. Siehe `docs/todos.md` für den vollständigen Stand.

### Dev-Tooling

Über **Tools → Test Infisical Connection** in der Sandbox-IDE lässt sich der Universal-Auth-Login
manuell gegen die echte Infisical-API testen (Client ID/Secret eingeben, Ergebnis erscheint als
Notification). Das ist eine temporäre Debug-Action und kein Nutzer-Feature — sie wird entfernt,
sobald eine echte Settings-UI/Tool-Window existiert.

## Claude-Workflow

Dieses Projekt verwendet einen strukturierten Claude-Workflow.
Siehe `CLAUDE.md` für Kontext und Konventionen.

Projektübersicht in Claude abrufen: `/abc-teamwork`
