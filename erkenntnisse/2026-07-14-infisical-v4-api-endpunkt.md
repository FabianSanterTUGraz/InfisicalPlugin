# Infisical-API: v4-Secrets-Endpoint und Imports-Verhalten

**TL;DR:** Der aktuelle Endpoint zum Abrufen von Secrets ist `GET /api/v4/secrets` (nicht das
ältere `/api/v3/secrets/raw`); die Response trennt eigene Secrets und importierte Secrets in zwei
separate Arrays, die nicht automatisch gemergt werden.

**TODO-Referenz:** docs/todos.md → #5

## Kontext

Für die Implementierungs-Spec zu TODO #5 (HTTP-Client für Secrets-Abruf) musste geklärt werden,
welcher Infisical-API-Endpoint aktuell der richtige ist, welche Parameter er erwartet und wie die
Response aufgebaut ist — bestehendes Wissen ging noch vom älteren `/api/v3/secrets/raw`-Endpoint
aus.

## Erkenntnis

Laut aktueller Doku (infisical.com/docs/api-reference, Stand 2026-07-14):

- **Endpoint:** `GET /api/v4/secrets` — der `/api/v3/secrets/raw`-Endpoint ist nicht mehr der
  empfohlene Weg.
- **Auth:** `Authorization: Bearer <token>` (Bearer-Auth-Schema).
- **Query-Parameter** (alle optional, exakte Schreibweise): `projectId` (nicht `workspaceId`!),
  `environment`, `secretPath` (Default `/`), `recursive` (Default `false`), `includeImports`
  (Default `true`), `viewSecretValue` (Default `true`), `expandSecretReferences` (Default `true`),
  `includePersonalOverrides` (Default `false`), `tagSlugs`, `metadataFilter`.
- **Response-Envelope:** `{"secrets": [...], "imports": [...]}` — zwei getrennte Top-Level-Arrays.
- **Secret-Objekt-Felder (Auszug):** `secretKey`, `secretValue` (nur befüllt wenn
  `viewSecretValue=true`), `secretComment`, `secretValueHidden` (Berechtigungs-Flag),
  `secretMetadata`, `tags`, `environment`/`workspace`/`secretPath` (Location-Metadaten).
- **Imports werden NICHT automatisch gemergt:** Jeder Eintrag im `imports`-Array hat die Form
  `{secretPath, environment, folderId, secrets: [...]}` — ein Client, der den vollen effektiven
  Secret-Satz eines Pfads will (wie in der Infisical-UI/CLI), muss `imports[].secrets` manuell mit
  dem Top-Level-`secrets`-Array zusammenführen. Das passiert serverseitig nicht.

**Entscheidung für Spec #5:** Für die erste Version wird `includeImports=false` gesetzt und das
`imports`-Array komplett ignoriert — vermeidet die Komplexität des verschachtelten Merge-Vorgangs.
Kann bei Bedarf später als Erweiterung nachgezogen werden, falls Projekte im Praxiseinsatz
tatsächlich Secrets-Imports zwischen Pfaden nutzen.
