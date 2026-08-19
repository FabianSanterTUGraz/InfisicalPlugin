# Secrets serverseitig als "maschinenspezifisch" taggen (Infisical API)

**TL;DR:** Tag-Erstellung und Tag-Zuweisung sind in der Infisical-API zwei getrennte Endpunkte,
die über die server-generierte Tag-UUID verknüpft werden. Der `GET /api/v4/secrets`-Endpunkt
liefert Tags pro Secret ohne Zusatzaufwand mit, was eine Idempotenz-Prüfung ohne weiteren
API-Call ermöglicht. Der Tagging-Pfad muss komplett fail-open sein, sonst wird das Laden der
Secrets selbst durch einen Tagging-Fehler blockiert — das war unterwegs eine echte Regression.

## Kontext

Secrets, deren Wert wie ein lokaler, maschinenspezifischer Dateipfad aussieht (z.B.
`C:\Users\...`), werden bereits lokal per `.infisical.local.json` überschrieben (Erkennung über
`looksLikeUserSpecificPath` im Code). Zusätzlich sollen solche Secrets serverseitig in
Infisical markiert werden, damit Teammitglieder sie in der Web-App leichter finden und dort
selbst einen Personal Override setzen können.

Betroffene Datei: `InfisicalPlugin/src/main/java/com/abuscom/infisicalplugin/infisical/cache/Cache.java`
(Methode `applyEnvironment`, Helper `resolveMachineSpecificTag`), zugehöriger Client in
`SecretClient.java`, Response-Model in `SecretEntry.java`/`TagListRequest`.

## Erkenntnis

### Tag-Erstellung und Tag-Zuweisung sind getrennte API-Operationen

- `POST /api/v1/projects/{projectId}/tags` (Body: `slug`, `color`) legt nur eine
  Tag-**Definition** im Projekt an. Die Response enthält u.a. eine server-generierte UUID (`id`).
- Das Zuweisen an ein konkretes Secret passiert separat über `PATCH /api/v4/secrets/{secretName}`
  mit `tagIds: [<id>]` im Body. Referenziert wird dabei die **UUID**, nicht der Slug.
- Ablauf im Code entsprechend zweistufig: zuerst `findTagBySlug` (existiert der Tag schon?),
  sonst `createTag`, danach erst `tagVariable` mit der zurückgegebenen `id()`.

### Tags kommen beim Secrets-Abruf bereits mit

- `GET /api/v4/secrets` liefert pro Secret standardmäßig ein `tags`-Array (`id`, `slug`, `color`,
  `name`) mit zurück — ohne Query-Parameter dafür extra anzufordern.
- Dadurch lässt sich Idempotenz ("ist dieses Secret schon getaggt?") komplett ohne
  zusätzlichen API-Call prüfen, einfach durch Auswertung der ohnehin schon geladenen Response
  (siehe `Cache.applyEnvironment`: `entry.tags().stream().anyMatch(t -> t.slug().equals(SLUG_NAME))`).
- Es gibt zusätzlich einen `tagSlugs`-Query-Parameter zum serverseitigen Filtern nach Tags —
  für diesen Use-Case nicht gebraucht, da ohnehin alle Secrets der Umgebung geladen werden.

### Bug: Singular- statt Plural-Feldname lässt Gson still scheitern

`SecretEntry` hatte ursprünglich ein Feld `String tag` (Singular) statt `List<TagResponse> tags`
(Plural, Array). Gson matched Feldnamen 1:1 gegen die JSON-Response — bei einem Namens-Mismatch
gibt es **keinen Fehler**, das Feld bleibt einfach `null`. `entry.tag()` war dadurch immer
`null`, obwohl der Server das `tags`-Array korrekt zurückgab.

Lektion: Beim Modellieren von API-Response-Records immer den exakten Feldnamen aus der
Doku/einem echten Response-Sample verwenden (Singular/Plural, Groß-/Kleinschreibung) — ein
Tippfehler dort erzeugt keinen Compile- oder Laufzeitfehler, nur stillen Datenverlust.

Aktueller Stand in `SecretEntry.java`:

```java
public record SecretEntry(String secretKey, String secretValue, int version, String id, List<TagResponse> tags) {
}
```

### Design-Entscheidung: Tagging muss fail-open sein

Eine erste Implementierung rief `findTagBySlug`/`createTag`/`tagVariable` ungefangen direkt in
`Cache.applyEnvironment(...)` auf. Das verursachte eine echte Regression: Zwei vorher grüne
Tests in `CacheEnvironmentSwitchTest` (die nur den `/api/v4/secrets`-Endpunkt stubben, nicht den
Tags-Endpunkt) schlugen mit einer 404-`InfisicalHttpException` fehl, weil `applyEnvironment`
jetzt **immer zuerst** einen Tag-Lookup macht, bevor überhaupt Secrets geladen werden.

Lehre: Tagging ist ein Nice-to-have (bessere Auffindbarkeit in der Web-App), das Laden der
Secrets ist die Kernfunktion des Plugins. Ein Fehler im Tagging-Pfad darf das Laden der Secrets
niemals verhindern. Umgesetzte Lösung:

- Der komplette Tag-Lookup/-Create/-Assign-Pfad liegt in `resolveMachineSpecificTag(...)` und ist
  dort per try/catch abgesichert; im Fehlerfall wird geloggt (`LOG.warn(...)`) und `null`
  zurückgegeben, statt zu werfen.
- Aufrufer in `applyEnvironment` prüft `tag != null` und überspringt das Tagging für diesen
  Durchlauf einfach, wenn die Tag-Auflösung fehlgeschlagen ist.
- Auch der einzelne `tagVariable`-Aufruf pro Secret ist zusätzlich try/catch-abgesichert, damit
  ein fehlschlagendes Tagging eines einzelnen Secrets nicht die restlichen Secrets der Schleife
  blockiert.

## Offene Fragen

- Die Struktur der `POST /api/v1/projects/{projectId}/tags`-Response (ob das Tag-Objekt direkt
  zurückkommt oder in einem Wrapper-Feld wie `{"tag": {...}}` steckt) wurde nur gegen die
  öffentliche Doku/Community-Wissen verifiziert, nicht gegen die echte, selbstgehostete
  Infisical-Instanz. Falls das beim ersten echten Testlauf nicht passt, ist das der erste Ort
  zum Nachschauen (`SecretClient.createTag`/`TagListRequest`).
