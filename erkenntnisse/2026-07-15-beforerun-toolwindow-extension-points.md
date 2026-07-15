# IntelliJ-Plugin: BeforeRunTaskProvider und Tool-Window korrekt in plugin.xml registrieren

**TL;DR:** Der Extension Point für `BeforeRunTaskProvider`-Implementierungen heißt
`stepsBeforeRunProvider` (nicht `beforeRunTaskProvider`) und nutzt das Attribut `implementation`
(nicht `implementationClass`); zusätzlich sollte das Marketplace-Icon (`pluginIcon.svg`, 40×40)
nie für kleine UI-Icons (Vor-Start-Liste, Tool-Window-Stripe) wiederverwendet werden. `icon` muss
außerdem als Attribut gesetzt werden, nicht als Kind-Element, und fehlende Extension-Registrierungen
(z.B. `notificationGroup`) scheitern lautlos zur Laufzeit statt beim Plugin-Start.

## Kontext

Beim Bau der Grundgerüste für TODO #8 (Secrets als Env-Vars vor einem Gradle-Run injizieren,
`InjectBeforeRunTask`) und TODO #9 (Tool-Window rechts, `InfisicalToolWindowFactory`) mussten beide
Klassen in `plugin.xml` als Plattform-Extensions registriert werden. Dabei traten zwei nicht
offensichtliche Stolpersteine auf, die sich beide erst durch Gegenprüfung mit dem
intellij-community-Quellcode auflösen ließen.

## Erkenntnis

**1. Extension-Point-Name und -Attribut für BeforeRunTaskProvider:**

Der naheliegende Name `<beforeRunTaskProvider implementation="...">` (abgeleitet vom
Java-Klassennamen `BeforeRunTaskProvider`) ist falsch. Aus dem Quellcode von
`com.intellij.execution.BeforeRunTaskProvider` (Paket `com.intellij.execution`,
`platform/execution/src/com/intellij/execution/BeforeRunTaskProvider.java`) geht hervor:

```java
EP_NAME = new ProjectExtensionPointName<>("com.intellij.stepsBeforeRunProvider")
```

Das korrekte XML-Tag lautet also `<stepsBeforeRunProvider .../>`, registriert im
`<extensions defaultExtensionNs="com.intellij">`-Block. Der EP ist mit
`interface="com.intellij.execution.BeforeRunTaskProvider"` deklariert; das Attribut zur Angabe der
Implementierungsklasse heißt **`implementation`**, nicht `implementationClass` — Letzteres führt zu
"Attribute ... is not allowed here"-Diagnosemeldungen im plugin.xml-Editor, weil die Plattform gar
keinen EP mit diesem Namen/dieser Attributsignatur kennt.

`BeforeRunTaskProvider<T extends BeforeRunTask<?>>` verlangt die Methoden `getId()`, `getName()`,
`createTask(RunConfiguration)` und `executeTask(DataContext, RunConfiguration, ExecutionEnvironment, T)`.

**2. Paket-Pfade für Gradle-Run-Config-Zugriff:**

- `GradleRunConfiguration` liegt unter
  `org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration`.
- `ExternalSystemTaskExecutionSettings` (mit `getEnv()`/`setEnv(Map)`,
  `setPassParentEnvs(boolean)`) liegt unter
  `com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings`.
- Die Plugin-ID für `<depends>` in `plugin.xml` und `bundledPlugin(...)` in `build.gradle.kts`
  lautet **`com.intellij.gradle`** — nicht `org.jetbrains.plugins.gradle`. Letzteres ist
  mittlerweile die separate Java-Gradle-Integration (eigenes Modul, eigene Plugin-ID), obwohl die
  Java-Package-Namen (`org.jetbrains.plugins.gradle.*`) aus Kompatibilitätsgründen unverändert
  geblieben sind. Verwechslungsgefahr, weil Plugin-ID und Java-Package-Präfix hier auseinanderlaufen.

**3. Icon-Wiederverwendung führt zu überdimensionierten UI-Listen:**

`src/main/resources/META-INF/pluginIcon.svg` ist das Marketplace-Icon des Plugins (Konvention:
40×40, `viewBox="0 0 40 40"`, sichtbar z.B. in *Settings → Plugins*). Für kleine UI-Icons erwartet
die Plattform deutlich kleinere Formate — 16×16 für Einträge in der "Before Launch"-Liste,
13×13 für Tool-Window-Stripe-Icons. Wird `pluginIcon.svg` direkt per
`IconLoader.getIcon(path, class)` für `getIcon()` in `BeforeRunTaskProvider` oder
`ToolWindowFactory` wiederverwendet, richtet Swing die Zeilenhöhe der gesamten Liste am größten
enthaltenen Icon aus — dadurch wirken plötzlich *alle* Einträge der Liste überdimensioniert, nicht
nur der eigene.

**Empfehlung:** Für kleine UI-Kontexte ein separates, korrekt dimensioniertes SVG anlegen (z.B.
`icons/infisical.svg` mit `width="16" height="16" viewBox="0 0 16 16"`) statt `pluginIcon.svg` zu
recyceln.

**4. `icon` muss Attribut sein, nicht Kind-Element — und `ToolWindowFactory.icon` ist keine Alternative:**

`<toolWindow icon="...">` erwartet `icon` als Attribut direkt am Tag, analog zu `id`/`anchor`/
`factoryClass`. Ein `<icon>`-Kind-Element (z.B. `<toolWindow ...><icon>/META-INF/x.svg</icon></toolWindow>`)
wird von der Plattform ignoriert — insbesondere wenn `toolWindow` dabei zusätzlich mit `/>`
selbst-geschlossen wird, landet das `<icon>`-Element sogar völlig losgelöst als eigenständiges
Geschwister-Element im `<extensions>`-Block. Alternativ ließe sich vermuten, man könne das Icon
stattdessen programmatisch über `ToolWindowFactory` liefern: Die Klasse hat tatsächlich eine
`icon`-Property (verifiziert im Quellcode, `platform/platform-api/.../ToolWindowFactory.kt`), diese
ist aber mit `@get:Internal` markiert — ein rein plattforminternes Implementierungsdetail. Ein
Java-`@Override public Icon getIcon()` kompiliert zwar (die Property hat einen entsprechenden
Getter), wird von der Tool-Window-Icon-Auflösung aber nicht berücksichtigt. Der einzige unterstützte
Weg ist das `icon`-Attribut in der `plugin.xml`-Registrierung (`ToolWindowEP`).

**5. Fehlende Extension-Registrierung scheitert lautlos zur Laufzeit, nicht beim Plugin-Start:**

`NotificationGroupManager.getInstance().getNotificationGroup(id)` gibt `null` zurück, wenn die
passende `<notificationGroup id="...">`-Registrierung in `plugin.xml` fehlt (z.B. weil sie beim
Umbau eines `<extensions>`-Blocks versehentlich mitgelöscht wurde). Es gibt dabei keinen Fehler
beim Plugin-Start — erst der nächste Aufruf einer Methode auf dem `null`-Ergebnis
(`.createNotification(...)`) wirft eine `NullPointerException`, weit entfernt vom eigentlichen
Ursprung des Problems. Genereller Fall: Fehlende oder gelöschte Extension-Registrierungen zeigen
sich in der Regel nicht beim Laden des Plugins, sondern erst spät und schwer nachvollziehbar an der
Aufrufstelle.

## Offene Fragen

Der Icon-Fix ist inzwischen umgesetzt (`pluginIconBlackWhite.svg`, 24×24, monochrom, korrekt als
Attribut verdrahtet). Offen ist, ob sich Tippfehler in `plugin.xml`-Extension-Registrierungen
(falsche EP-Namen/Attribute, versehentlich gelöschte Blöcke) künftig automatisiert absichern lassen
— z.B. durch einen Plugin-Verifier-Lauf oder einen einfachen Smoke-Test, der beim Plugin-Start alle
erwarteten Extensions (`notificationGroup`, `toolWindow`, `stepsBeforeRunProvider`) auf Vorhandensein
prüft, statt solche Fehler erst manuell beim Ausprobieren zu entdecken.