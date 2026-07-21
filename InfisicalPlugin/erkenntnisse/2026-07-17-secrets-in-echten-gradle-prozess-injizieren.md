# Secrets in den echten Gradle-Prozess injizieren (nicht nur ins UI-Feld)

**TL;DR:** `RunConfigurationExtension.patchCommandLine` funktioniert für Gradle-Runs nicht, weil
Gradle-Ausführung nicht über `GeneralCommandLine` läuft. Der naheliegende Ersatz
(`ExternalSystemTaskExecutionSettings.setEnv()`) landet zwar im echten Prozess, mutiert aber die
persistierte Run Configuration selbst. Der Kandidat für "unsichtbar wie im Terminal" ist
`GradleExecutionHelperExtension.configureOperation(LongRunningOperation, ...)`.

## Kontext

Ziel: Infisical-Secrets sollen beim Start eines Gradle-Runs (grüner Play-Button) so in den
Prozess gelangen, dass `System.getenv("KEY")`/`echo $KEY` innerhalb des Gradle-Builds den
richtigen Wert sieht — genau wie wenn man den Wert vorher im Terminal exportiert und dann
`./gradlew` aufgerufen hätte. Explizit NICHT gewünscht: dass der Wert im "Environment
Variables"-Feld der Run Configuration sichtbar auftaucht oder beim Speichern der Config auf die
Platte geschrieben wird.

## Erkenntnis

### Ansatz 1 (verworfen): `RunConfigurationExtension.patchCommandLine`

`com.intellij.execution.RunConfigurationExtension` (SDK, im Java-Plugin `java-impl.jar`) bietet
`patchCommandLine(RunConfigurationBase, RunnerSettings, GeneralCommandLine, String)` — patcht
`GeneralCommandLine.getEnvironment()`, die reale Env-Map eines per `ProcessBuilder` gestarteten
OS-Prozesses.

Für Gradle-Run-Configs greift das nicht: Die Klasse, die einen Gradle-Task tatsächlich ausführt
(`com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunnableState`, in
`app.jar`, per `javap` decompiliert) implementiert `RunProfileState` direkt und enthält an
keiner Stelle ein `GeneralCommandLine`-Objekt. Die Ausführung läuft komplett über die Gradle
Tooling API (`ExternalSystemExecuteTaskTask`/`ProjectConnection`), nicht über einen von IntelliJ
selbst gestarteten OS-Prozess. `patchCommandLine` wird für Gradle-Configs schlicht nie
aufgerufen.

### Ansatz 2 (funktioniert, aber mit Seiteneffekt): `ExecutionListener` + `settings.setEnv()`

`com.intellij.execution.ExecutionListener.processStartScheduled(executorId, env)` feuert vor dem
Start. `env.getRunProfile()` liefert die `ExternalSystemRunConfiguration`-Instanz; darauf
`getSettings().setEnv(...)` zu rufen (implementiert in `InterceptGradleRunConfig.java`) landet
tatsächlich im echten Prozess: `GradleExecutionHelper.setupEnvironment(LongRunningOperation,
GradleExecutionSettings)` liest genau dieses `env`-Feld aus und reicht es über
`LongRunningOperation.setEnvironmentVariables(...)` (Gradle Tooling API) an den echten
Gradle-Daemon-Prozess weiter — verifiziert durch Decompilieren von `GradleExecutionHelper` in
`plugins/gradle/lib/gradle.jar`.

Das Problem: `gradleConfig.getSettings()` ist das **live, persistierte**
`ExternalSystemTaskExecutionSettings`-Objekt der Run Configuration — exakt das, was im
"Environment Variables"-Textfeld im `Edit Configurations`-Dialog angezeigt wird. Die Secrets
werden dadurch dort sichtbar und würden bei einem Speichern der Config im Klartext in
`.idea/workspace.xml`/`.run/*.xml` landen. Technisch "im echten Prozess", aber mit einem
unerwünschten UI-/Persistenz-Leck.

### Ansatz 3 (aktuell verfolgt): `GradleExecutionHelperExtension`

Im Gradle-Plugin selbst gibt es einen dafür vorgesehenen, dynamischen Extension Point:

```java
package org.jetbrains.plugins.gradle.service.project;

public interface GradleExecutionHelperExtension {
    ExtensionPointName<GradleExecutionHelperExtension> EP_NAME;
    // qualifiedName = "org.jetbrains.plugins.gradle.executionHelperExtension"

    default void configureSettings(GradleExecutionSettings settings, GradleExecutionContext context) {}
    default void configureOperation(LongRunningOperation operation, GradleExecutionContext context) {}
    default void prepareForExecution(ExternalSystemTaskId taskId, LongRunningOperation operation,
                                      GradleExecutionSettings settings, BuildEnvironment buildEnvironment) {}
}
```

`configureOperation` bekommt das rohe Tooling-API-`LongRunningOperation` mit direktem Zugriff
auf `setEnvironmentVariables(...)` — dieses Objekt ist pro Ausführung frisch aufgebaut, komplett
losgelöst von der persistierten Run Configuration. Env hier setzen heißt: landet im echten
Prozess, taucht aber nirgendwo in der UI auf und wird nirgendwo persistiert. Das ist der
gesuchte "wie im Terminal exportiert"-Mechanismus.

`GradleExecutionContext` (ebenfalls decompiliert) stellt bereit: `getProject()`,
`getProjectPath()`, `getTaskId()` (`ExternalSystemTaskId`), `getSettings()`
(`GradleExecutionSettings`), `getListener()`, `getCancellationToken()`, `getBuildEnvironment()`,
`getGradleVersion()`.

## Offene Fragen

- `GradleExecutionHelperExtension` feuert für **jede** Gradle-Ausführung im Projekt (auch
  Projekt-Sync, "Reload Gradle Project", von Tests ausgelöste Gradle-Runs) — nicht nur für den
  einen Run über den grünen Play-Button einer Run Configuration mit aktivierter Checkbox.
  `GradleExecutionContext` liefert keinen direkten Rückverweis auf die auslösende
  `RunConfiguration`/`ExecutionEnvironment`. Wie wird korreliert, für welche konkrete Ausführung
  injiziert werden soll (Kandidat: kurzlebige In-Memory-Registrierung, angestoßen von
  `ExecutionListener.processStartScheduled`, per Projektpfad korreliert, "consume once")?
- Exakter Setter-Name für Env auf `GradleExecutionSettings` (falls `configureSettings` statt
  `configureOperation` verwendet wird) noch nicht verifiziert.
- Ob die bestehende `<depends>com.intellij.gradle</depends>` ausreicht, um den Extension Point
  `org.jetbrains.plugins.gradle.executionHelperExtension` zu nutzen, oder ob eine andere
  Plugin-Abhängigkeit nötig ist.
- Race-Bedingung bei mehreren gleichzeitigen Gradle-Ausführungen im selben Projektpfad noch nicht
  bewertet (für "erster Schritt" evtl. akzeptabel).