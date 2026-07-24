# Fehlermeldungen

**TL;DR:** Übersicht aller Fehlermeldungen, die das Plugin als IDE-Notification anzeigt, mit
Bedeutung und Auslöser.

## Kontext

Alle Fehler laufen über `ErrorNotifier` (`errorMessages/ErrorNotifier.java`) und werden als
IntelliJ-Notification mit Titel **Infisical** und Typ `ERROR` angezeigt. Die eigentliche
Nutzertext-Übersetzung für HTTP-Fehler liegt in `InfisicalHttpException.getUserMessage()`.

## Übersicht

| Meldung | Bedeutung | Ausgelöst durch |
|---|---|---|
| Ungültiger oder abgelaufener Token — bitte erneut einloggen. | Der gespeicherte JWT ist abgelaufen oder wurde von Infisical abgelehnt (HTTP 401/403). | `InfisicalHttpException.getUserMessage()` bei Statuscode 401/403 |
| No valid jwt-Token given!(not logged in or expired) | Es ist noch kein Token gespeichert, oder `TokenManager.isTokenValid()` hat ihn als abgelaufen erkannt, *bevor* überhaupt eine Anfrage an Infisical geschickt wurde. | `InjectSecretsRunConfigurationExtension` / `InjectIntoNpmProcess`, direkter `ErrorNotifier.notify(project, String)`-Aufruf |
| Infisical ist nicht erreichbar (Netzwerkfehler). | Die Verbindung zu Infisical ist fehlgeschlagen (Timeout, DNS, Server down, kein Netz). | `InfisicalHttpException.getUserMessage()` bei Statuscode -1 (gewrappte `IOException`/`InterruptedException` aus `InfisicalHttpClient`) |
| Infisical-Anfrage fehlgeschlagen (Status X). | Infisical hat mit einem anderen Fehlerstatus als 401/403 geantwortet (z.B. 500 bei einem Server-Fehler). | `InfisicalHttpException.getUserMessage()`, Fallback-Fall |
| Infisical-Konfiguration (.infisical.json) konnte nicht gelesen werden: ... | Die `.infisical.json` im Projekt-Root fehlt, ist nicht lesbar oder kein valides JSON. | `ErrorNotifier`, `IOException`-Fall aus `Cache.readConfig(...)` |
| Unerwarteter Fehler: ... | Fängt jede sonstige, nicht speziell behandelte Exception ab. | `ErrorNotifier`, Fallback-Fall für alle anderen `Throwable`-Typen |

## Offene Fragen

- Die Token-Meldung aus `InjectSecretsRunConfigurationExtension`/`InjectIntoNpmProcess` ist auf
  Englisch, alle anderen auf Deutsch — sollte vereinheitlicht werden (vermutlich auf Deutsch, wie
  der Rest der Notifications).
