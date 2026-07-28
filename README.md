# Plugin Installation & Nutzung

> **Unterstützte IDEs:** IntelliJ IDEA Ultimate (Gradle/Spring-Boot-Integration). Für die
> npm-Integration wird zusätzlich das gebundelte NodeJS-Plugin benötigt (in IntelliJ IDEA
> Ultimate bereits enthalten, z. B. auch über WebStorm nutzbar).

## Funktionalitäten

- **Browser-basierter Login:** Anmeldung erfolgt einmalig über die Infisical-Weboberfläche; das Plugin startet dafür kurzzeitig einen lokalen Callback-Server (`localhost:8010`), der das JWT-Token entgegennimmt.
- **Sichere Token-Speicherung:** Das Token wird über die IntelliJ PasswordSafe-API gespeichert, nicht im Klartext.
- **Automatische Ablaufprüfung:** Vor jedem Start wird geprüft, ob das gespeicherte Token noch gültig ist (JWT-Expiry). Ist es abgelaufen, wird es automatisch verworfen und die Login-Option erscheint wieder.
- **Environment-Auswahl:** Über ein Dropdown in der Run-Configuration (bzw. im Infisical-Tab bei npm) lässt sich pro Konfiguration die gewünschte Umgebung (z. B. `dev`, `prod`) auswählen.
- **Automatische Secret-Injection:** Beim Start einer aktivierten Run-Configuration (Gradle/SpringBoot oder npm/Node) werden die Secrets des gewählten Environments automatisch als Umgebungsvariablen in den gestarteten Prozess injiziert — keine lokalen `.env`-Dateien mehr nötig.
- **Caching mit Versionsabgleich:** Die Secrets werden nicht bei jedem Start neu von der Infisical-Cloud geladen. Stattdessen wird vor jedem Start nur ein leichtgewichtiger Metadaten-Abruf gemacht, der die Versionsnummern der Secrets vergleicht — nur bei tatsächlicher Änderung (oder Umgebungs-Wechsel) werden die vollständigen Secrets neu geladen. Das ist kein Live-Push aus der Web-UI, sondern ein Abgleich, der bei jedem Programmstart passiv mit ausgelöst wird.
- **Lokale Overrides (`.infisical.local.json`):** Für maschinenspezifische Pfade lassen sich einzelne Secret-Werte lokal überschreiben, ohne die zentralen Cloud-Werte zu verändern; diese Overrides überleben einen Environment-Wechsel.
- **Fehlerbehandlung über Notifications:** Fehlende `.infisical.json`, ungültiges/abgelaufenes Token oder HTTP-Fehler bei der API führen zu einer IDE-Benachrichtigung statt zu einem Absturz oder stillem Fehlschlag; bei Gradle-Tasks wird der Task zusätzlich automatisch abgebrochen, damit nicht versehentlich ohne Secrets weitergebaut/gestartet wird.
- **Pro-Run-Configuration persistierte Einstellungen:** Ob Infisical aktiviert ist und welches Environment gewählt wurde, wird pro Run-Configuration gespeichert und bleibt über IDE-Neustarts hinweg erhalten.

## Installation

1. In der IDE: **File > Settings > Plugins** → Zahnrad-Symbol (⚙) oben rechts → **Manage Plugin Repositories...**

   ![Alt-Text](ExampleScreenshots/woInstallieren.png)
2. Über `+` folgende URL eintragen (einmalig):

   ```
   https://gitlab.abuscom.cloud/api/v4/projects/255/packages/generic/infisical-plugin/repository/updatePlugins.xml
   ```
   ![Alt-Text](ExampleScreenshots/custompluginrepositories.png)

3. Im Reiter **Marketplace** nach `InfisicalPlugin` suchen und **Install** klicken.

4. IDE neu starten und prüfen, ob das Plugin unter **Plugins** aufgeführt wird.

Zukünftige Updates werden ab dann automatisch von der IDE erkannt und müssen nicht mehr manuell nachinstalliert werden.

## Infisical CLI installieren

Um Infisical selbst zu installieren (welche man im nächsten Schritt braucht) kann es mit npm folgendermaßen installiert werden:

```
npm install -g @infisical/cli
npm update -g @infisical/cli
```

Alternativ alle Installationsmöglichkeiten: https://infisical.com/docs/cli/overview#debian%2Fubuntu

## Verbindung mit der abuscom instanz

Damit man im nächsten schritt die init funktion aufrufen kann muss sich erstmals über die infisical CLI mit dem abuscom.internal server zu verbinden.

![Alt-Text](ExampleScreenshots/anmeldung_cli.png)

Entweder bereits bestehende internal domain auswählen oder diese hinzufügen:

```
   https://infisical.internal.abuscom.cloud
   ```

![Alt-Text](ExampleScreenshots/select_domain.png)

Dann wird man umgeleitet um sich einmalig anzumelden. 

## Konfiguration: `.infisical.json`

Damit das Plugin funktioniert, muss im Projekt-Root eine `.infisical.json`-Datei mit folgendem Format vorhanden sein:

```json
{
  "workspaceId": "...…..-....-....-....-............",
  "defaultEnvironment": "dev",
  "gitBranchToEnvironmentMapping": null
}
```

Diese Datei wird über folgenden Befehl erzeugt:

```bash
infisical init
```

Anschließend den Anweisungen im Terminal folgen, um die Verbindung herzustellen.

> **Hinweis:** Diese Datei enthält keine sensiblen Daten und kann daher problemlos ins Repository eingecheckt werden.

## Spezieller Fall für hardcoded machine paths

Manche Werte haben die Form eines lokalen, maschinenspezifischen Pfads (z. B. `C:/Users/...`, `/Workspace/abuscom/...`) und müssen daher für den eigenen Rechner individuell gesetzt werden. Das Plugin erkennt solche Fälle automatisch und legt im selben Verzeichnis wie die `.infisical.json`-Datei eine zusätzliche Datei namens `.infisical.local.json` an. Dort werden diese Werte manuell eingetragen.

Die lokalen Werte haben Vorrang vor den Werten aus Infisical — damit lässt sich bei Bedarf auch temporär ein einzelner Wert überschreiben. Beim Wechsel des Environments bleiben die lokalen Werte erhalten und müssen beim Zurückwechseln nicht erneut eingetragen werden.

Die `.infisical.local.json` hat folgende Form:

```json
{
  "TEST_CONFIG_REPOSITORY_ROOT_DIRECTORY": "",
  "TEST_CONFIG_REPOSITORY_OTHER_DIRECTORY": "",
  "TEST_CONFIG_DIRECTORY": ""
}
```

Das ist ein sehr spezieller Anwendungsfall und sollte selten vorkommen.

> **Hinweis:** Diese Datei gehört ins `.gitignore` und darf keinesfalls eingecheckt werden (sie enthält Geheimnisse wie eine `.env`-Datei).


## Für Gradle/SpringBoot

1. Bestehende Gradle-Run-Configuration aktivieren: Drei-Punkte-Menü neben dem Debug-Symbol → **Edit Configurations** → 
**Modify Options** → Haken bei **Infisical** setzen.

   ![Alt-Text](ExampleScreenshots/pluginAktivieren.png)
2. Danach erscheinen die Login-Option sowie eine Checkbox, mit der Infisical aktiviert oder pausiert werden kann.
   ![Alt-Text](ExampleScreenshots/loginfeld.png)
3. Über das Login-Feld wirst du zur Login-Seite weitergeleitet, auf der du dich mit deinen Zugangsdaten anmeldest.
   ![Alt-Text](ExampleScreenshots/login.png)
4. Nach dem Login kannst du über das Dropdown-Menü die gewünschte Umgebung (Environment) auswählen.
   ![Alt-Text](ExampleScreenshots/dropdownmenu.png)

Sobald das Plugin aktiviert ist, können Gradle-Projekte wie gewohnt über **Run** gestartet werden. Lokale Environment-Dateien können vollständig aus dem Projektordner gelöscht werden, da die Werte jetzt über Infisical bereitgestellt werden.


## Für npm

Steuerung bleibt die gleiche jedoch muss Infisical nicht manuell aktiviert werden sondern kann direkt über den Reiter **Infisical** neben **Browser / Live Edit** aufgerufen werden.
   ![Alt-Text](ExampleScreenshots/howtoNPM.png)

Nach Aktivierung sind die Schritte die selbe wie bei Gradle.
> Ausführung über das terminal via npm start wird nicht unterstützt! 

## Fehlermeldungen

Eine Übersicht aller Fehlermeldungen des Plugins und was sie bedeuten findest du in
[docs/error-messages.md](docs/error-messages.md).

## Entwicklung

Für Beiträge am Plugin selbst (nicht nur die Nutzung):

1. Projekt in IntelliJ IDEA Ultimate als Gradle-Projekt öffnen (Verzeichnis `InfisicalPlugin/`).
2. Plugin in einer Sandbox-IDE starten: `./gradlew runIde`.
3. Tests ausführen: `./gradlew test`.
4. Vor jedem Release-Tag den Plugin-Verifier laufen lassen: `./gradlew verifyPlugin`.

Details zu CI/CD und Release-Ablauf siehe `.gitlab-ci.yml` sowie den `/version`-Skill.

## Technischer Hintergrund: Wie sich das Plugin in die Run-Prozesse einhakt

Die drei unterstützten Run-Config-Typen (Gradle, native Spring-Boot-Configs, npm/Node) starten ihre Prozesse intern auf drei völlig unterschiedliche Arten. Deshalb nutzt das Plugin für jeden Typ einen eigenen, dafür passenden Extension-Point statt eines einzigen generischen Hooks.

### Gradle (`ExternalSystemRunConfiguration`)

Ein Gradle-Task wird nicht als einfacher Java-Prozess gestartet, sondern über die Gradle Tooling API an einen Gradle-Daemon delegiert. Es gibt dabei nie ein `JavaParameters`-Objekt, das am Ende in eine `GeneralCommandLine` übersetzt wird — der generische `RunConfigurationExtension`-Callback `updateJavaParameters` wird für diesen Weg von der Plattform schlicht nie aufgerufen, egal was `isApplicableFor` zurückgibt.

Der tatsächlich wirksame Hook ist deshalb `InjectIntoGradleProcess implements GradleExecutionHelperExtension`, registriert über den Gradle-Plugin-eigenen Extension-Point `org.jetbrains.plugins.gradle.executionHelperExtension`. Dessen `configureSettings(GradleExecutionSettings, GradleExecutionContext)` wird von `GradleExecutionHelper.setupEnvironment()` für jede Gradle-Ausführung aufgerufen, bevor die eigentliche Tooling-API-Operation losläuft. Da dieser Callback keine Referenz auf die RunConfiguration hat, liest er Checkbox/Dropdown-Auswahl nicht selbst aus, sondern aus dem statischen `Cache`-Singleton, in den `InjectSecretsRunConfigListener` (Event `processStartScheduled`) sie vorher geschrieben hat.

Bearbeitet wird konkret das `GradleExecutionSettings`-Objekt: für jedes Secret ruft der Code `settings.addEnvironmentVariable(key, value)` auf. Diese Env-Map übernimmt `GradleExecutionHelper` unverändert in die Tooling-API-Build-Operation, wodurch die Werte im gestarteten Gradle-Daemon-Prozess als Umgebungsvariablen ankommen.

### Spring Boot (`SpringBootApplicationRunConfiguration`)

Eine native Spring-Boot-Run-Configuration (also nicht per "Run using: Gradle" an Gradle delegiert) startet die kompilierte Main-Class direkt als Java-Prozess. Dafür baut die Plattform intern ein `JavaParameters`-Objekt, das anschließend in die tatsächliche `GeneralCommandLine` übersetzt wird — das ist genau der `JavaCommandLineState`-Weg, für den die generische `RunConfigurationExtension`-API gedacht ist.

`InjectSecretsRunConfigurationExtension.isApplicableFor(...)` liefert für diesen Konfigurationstyp `true`, `isEnabledFor(...)` liest dazu die persistierte Checkbox aus `InjectSecretsSettings`. Weil hier tatsächlich ein `JavaCommandLineState` durchlaufen wird, ruft die Plattform automatisch `updateJavaParameters(...)` auf, bevor aus den `JavaParameters` die finale Commandline gebaut wird. `patchCommandLine(...)` bleibt bewusst leer — die Injection läuft ausschließlich über die Umgebungsvariablen, nicht über CLI-Argumente.

Bearbeitet wird konkret `javaParameters.getEnv()` — eine simple `Map<String,String>`, in die `Cache.getInstance().getSecrets()` per `putAll(...)` geschrieben wird. Diese Map übernimmt die Plattform 1:1 als Umgebungsvariablen des gestarteten Java-Prozesses. Wichtig: Wird dieselbe Spring-Boot-Config stattdessen per "Run using: Gradle" ausgeführt, greift nicht dieser Weg, sondern der Gradle-Pfad oben, weil dann kein `JavaCommandLineState` mehr durchlaufen wird.

### npm / Node (`AbstractNodeTargetRunProfile`)

npm-/Node-Run-Configs laufen über die modernere "Targets API" des NodeJS-Plugins, die auch Remote-/WSL-/Docker-Ausführungsziele abstrahiert — kein `JavaCommandLineState`, kein `JavaParameters`. Weder der generische `com.intellij.runConfigurationExtension` noch dessen `patchCommandLine`/`updateJavaParameters` werden für diesen Config-Typ jemals aufgerufen.

Der Hook ist hier `InjectIntoNpmProcess extends AbstractNodeRunConfigurationExtension`, registriert über den NodeJS-Plugin-eigenen Extension-Point `JavaScript.nodeRunConfigurationExtension` (siehe `plugin.xml`, optionale Abhängigkeit `NodeJS` über `withJavascript.xml`). Statt `updateJavaParameters` überschreibt die Klasse `createLaunchSession(...)`, das eine `NodeRunConfigurationLaunchSession` liefert; deren `addNodeOptionsTo(NodeTargetRun targetRun)` ruft die Plattform kurz vor dem tatsächlichen Start des Node-Prozesses auf. Aktuell liefert `isApplicableFor(...)` hier laut Code-Kommentar (TODO) noch pauschal `true` für alle Node-Configs, statt wie bei Gradle/SpringBoot die Checkbox-Auswahl zu berücksichtigen.

Bearbeitet wird konkret das `NodeTargetRun`-Objekt: `targetRun.setEnvData(EnvironmentVariablesData.create(secrets, true))`. `EnvironmentVariablesData` ist derselbe Typ, den auch andere native IntelliJ-Run-Configs für Umgebungsvariablen verwenden; die Plattform wendet ihn beim tatsächlichen Start des Node-Prozesses an — unabhängig davon, ob dieser lokal oder auf einem Remote-Target läuft.

## Secrets-sammeln
Anbei eine Checkliste von allen Projekten wo die .env files in der infisical Cloud liegen:
[]
[]
[]
[]
[]
