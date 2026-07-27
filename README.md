# Plugin Installation & Nutzung

## Installation

1. In der IDE: **File > Settings > Plugins** → Zahnrad-Symbol (⚙) oben rechts → **Manage Plugin Repositories...**
2. Über `+` folgende URL eintragen (einmalig):

   ```
   https://gitlab.abuscom.cloud/api/v4/projects/255/packages/generic/infisical-plugin/repository/updatePlugins.xml
   ```

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

Entweder bereits bestehende internal domain auswäglen oder diese hinzufügen:

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

Steuerung bleibt die gleich jedoch muss Infisical nicht manuell aktiviert werden sondern kann direkt über den Reiter **Infisical** neben **Brower / Live Edit** aufgerufen werden.
   ![Alt-Text](ExampleScreenshots/howtoNPM.png)

Nach aktivierung sind die Schritte die selbe wie bei Gradle.
> Ausführung über das terminal via npm start wird nicht unterstützt! 

## Fehlermeldungen

Eine Übersicht aller Fehlermeldungen des Plugins und was sie bedeuten findest du in
[docs/error-messages.md](docs/error-messages.md).
