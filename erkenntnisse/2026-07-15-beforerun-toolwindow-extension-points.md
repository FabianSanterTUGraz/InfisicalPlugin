# IntelliJ-Plugin: BeforeRunTaskProvider und Tool-Window korrekt in plugin.xml registrieren

**TL;DR:** Der Extension Point für `BeforeRunTaskProvider`-Implementierungen heißt
`stepsBeforeRunProvider` (nicht `beforeRunTaskProvider`) und nutzt das Attribut `implementation`
(nicht `implementationClass`); zusätzlich sollte das Marketplace-Icon (`pluginIcon.svg`, 40×40)
nie für kleine UI-Icons (Vor-Start-Liste, Tool-Window-Stripe) wiederverwendet werden.

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

## Offene Fragen

Der Icon-Fix (separates klein dimensioniertes SVG für `getIcon()`) wurde noch nicht umgesetzt —
`InjectBeforeRunTask` und `InfisicalToolWindowFactory` verwenden im aktuellen Code-Stand weiterhin
`pluginIcon.svg`.