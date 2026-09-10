# Wartungsrisiken für den unbetreuten Langzeitbetrieb

**TL;DR:** Vollständiger Projekt-Scan vor Fabians Austritt (September 2026) identifiziert zehn
Wartungsrisiken, davon zwei kritisch: fehlendes `until-build` in `plugin.xml` lässt das Plugin bei
künftigen IntelliJ-Versionen still (statt sichtbar) brechen, und die Package Registry ist für
externe Nutzer weiterhin nicht erreichbar (TODO #27).

**TODO-Referenz:** docs/todos.md → #27

## Kontext

Fabian verlässt die Firma im September 2026; danach wird das Projekt voraussichtlich nicht mehr
aktiv gewartet. Ziel des Scans: alle Stellen identifizieren, an denen das Plugin über längere Zeit
unbemerkt kaputtgehen könnte oder an denen ein Problem auftritt, das ohne Fabians Wissen/Zugriff
nicht mehr behebbar ist.

Durchsucht wurden: `build.gradle.kts`, `gradle.properties`, `settings.gradle.kts`,
`libs.versions.toml`, `gradle-wrapper.properties`, beide `.gitlab-ci.yml`, `plugin.xml`
(Quelle + generiert), `README.md`, `CHANGELOG.md`, `SYSTEM_ARCHITEKTUR.md`, `ANSIBLE.md`,
`.gitignore`-Dateien, der komplette Java-Quellcode (Cache, HTTP-Client, Auth, alle
Injection-Mechanismen) sowie `.github/workflows` + `dependabot.yml`.

## Erkenntnis

### Kritisch

1. **Kein `until-build` in `plugin.xml`** — der `ideaVersion { }`-Block in `build.gradle.kts` ist
   leer, es wird nur `since-build="253"` generiert, keine Obergrenze. Das Plugin gilt damit für
   jede künftige IntelliJ-Version als kompatibel, auch wenn die bereits bekannte, bytecode-fragile
   Maven-Injection (verifiziert gegen 2025.3.5, laut SYSTEM_ARCHITEKTUR.md ab 2026.1
   migrationsbedürftig) dabei still bricht — keine sichtbare Fehlermeldung, nur stille
   Fehlfunktion. Mitigation: `until-build` auf die getestete Version cappen (z.B. `"253.*"`),
   damit Inkompatibilität sichtbar scheitert statt lautlos zu verstummen.
2. **Package Registry für Externe nicht erreichbar** (TODO #27, weiterhin offen) — 401 für jeden
   ohne Admin-Rechte auf `abuscom/infisicalplugin` (Projekt von Personal-Namespace 255 in die
   Gruppe verschoben). Einziger Distributionsweg, da noch kein Marketplace-Release existiert.

### Mittel

3. **npm-Injection ungefiltert** — `InjectIntoNpmProcess.isApplicableFor` liefert laut Code-TODO
   pauschal `true` statt die Nutzer-Checkbox auszuwerten (anders als bei Gradle/Spring Boot).
   Secrets können so in npm-Configs landen, bei denen der Nutzer Injection nie aktiviert hat.
4. **Maven-Injection schreibt in persistierbares Settings-Objekt** —
   `InjectSecretsRunConfigListenerMaven` mutiert `MavenRunnerSettings`, dasselbe Objekt, das
   IntelliJ ggf. in `.idea/runConfigurations/*.xml` serialisiert. In SYSTEM_ARCHITEKTUR.md als
   Einschränkung dokumentiert, aber nirgends im nutzerorientierten README als Warnung — bei
   fehlendem `.idea/`-Gitignore in Consumer-Repos können Secrets im Klartext landen.
5. **CHANGELOG.md enthält seit dem ersten Eintrag nur `[Unreleased]`**, obwohl laut
   `docs/todos.md` bereits mehrere Versionen (z.B. v1.0.8) getaggt wurden. JetBrains erwartet für
   das Marketplace-Listing (TODO #26) ein gepflegtes Changelog.
6. **Tote GitHub-Actions-Infrastruktur** (`.github/workflows/release.yml`, `dependabot.yml`) —
   unverändertes Boilerplate des offiziellen IntelliJ-Plugin-Templates für Marketplace-Publish via
   GitHub (inkl. Signing-Secrets `PUBLISH_TOKEN`/`CERTIFICATE_CHAIN`/`PRIVATE_KEY`), während der
   echte Release über die eigene `.gitlab-ci.yml` in die GitLab Package Registry läuft.
   Vermutlich funktionslos, kann aber einen Nachfolger verwirren, der TODO #26 angeht und diesen
   Workflow für den echten Mechanismus hält.
7. **`ANSIBLE.md` verweist auf veralteten persönlichen Namespace** —
   `gitlab.abuscom.cloud/fabian.santer/infisicalplugin` statt der aktuellen Gruppen-URL
   `abuscom/infisicalplugin`. Persönliche Namespaces werden bei Account-Deaktivierung oft
   mit-deaktiviert, der Link kann nach Fabians Austritt ins Leere laufen.
8. **`Cache` ist ein projektübergreifendes Singleton** für Secrets/Environment-Auswahl —
   theoretisches Race-Condition-Risiko bei mehreren gleichzeitig offenen IDE-Fenstern/Projekten.
   Kein akutes Ticket, aber eine architektonische Design-Entscheidung, die nirgends in
   SYSTEM_ARCHITEKTUR.md dokumentiert ist.

### Niedrig

9. `build.gradle.kts` enthält 8 `runIde`-Testregistrierungen mit hartcodierten Pfaden
   (`C:/Users/Abuscom/workspace/...`) — funktionieren nur auf Fabians Rechner, reine
   Onboarding-Reibung, kein Produktionsrisiko.
10. README-Abschnitt "Secrets-sammeln" endet mit vier leeren `[]`-Platzhaltern ohne erkennbaren
    Zweck.

### Bereits bekannt (nicht erneut vertieft)

- GitLab-Pages-Fehlkonfiguration (`$CI_PAGES_URL` zeigt auf Platzhalter-Domain)
- Projektweites statt pro-Run Infisical-Injection-Gating (Fehler-Spam bei unrelated Task-Runs)
- Verbreiterte User-Pfad-Erkennung (matched jeden absoluten Pfad statt nur Users/Home-Ordner)

### Positiv verifiziert

Alle Build-/Plugin-Versionen (Gradle 9.6.1, IntelliJ Platform Gradle Plugin 2.18.1,
Changelog-Plugin 2.5.0, JUnit) sind exakt gepinnt, keine floatenden Ranges — Build-Reproduzierbarkeit
ist solide. Auch die Dokumentation (SYSTEM_ARCHITEKTUR.md, erkenntnisse/) ist für ein
Ein-Personen-Projekt ungewöhnlich gründlich.

## Offene Fragen

- Soll vor Fabians Austritt noch jemand mit GitLab-Owner/Maintainer-Rechten die
  Package-Registry-Sichtbarkeit fixen (→ TODO #27)?
- Wer übernimmt künftig das JetBrains-Marketplace-Publishing (TODO #26) — wessen Account wird
  dafür genutzt, und ist das mit einem persönlichen oder einem Team-/Service-Account verknüpft?
