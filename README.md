# Plugin Installation & Nutzung

## Installation

1. In der IDE: **File > Settings > Plugins** → Zahnrad-Symbol (⚙) oben rechts → **Manage Plugin Repositories...**

2. Über `+` folgende URL eintragen (einmalig):

   ```
   https://<eure-pages-url>/updatePlugins.xml
   ```

3. Im Reiter **Marketplace** nach `InfisicalPlugin` suchen und **Install** klicken.

4. IDE neu starten und prüfen, ob das Plugin unter **Plugins** aufgeführt wird.

Zukünftige Updates werden ab dann automatisch von der IDE erkannt und müssen nicht mehr manuell nachinstalliert werden.

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

## Für Gradle

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

## Für Maven