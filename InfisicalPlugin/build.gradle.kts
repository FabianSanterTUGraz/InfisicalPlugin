import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType


        plugins {
    id("java")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit4)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly(libs.junit.vintage.engine)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2025.3.5")
        testFramework(TestFrameworkType.Platform)

        // Add plugin dependencies for compilation here:
        bundledPlugin("com.intellij.modules.json")
        bundledPlugin("com.intellij.java")
        bundledPlugin("com.intellij.gradle")
        bundledPlugin("org.jetbrains.plugins.terminal")
        bundledPlugin("JavaScript")
        bundledPlugin("com.intellij.spring.boot")
        bundledPlugin("org.jetbrains.idea.maven")

    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}

// Deaktiviert, da die Legacy-Ant-Instrumentierung (Javac2) unter Windows-JDKs (z.B. .jdks/ms-25.0.3)
// einen nicht existierenden "Packages"-Unterordner erwartet und mit einem Build-Fehler abbricht.
intellijPlatform {
    instrumentCode = false

    // Ohne untilBuild testet verifyPlugin per Default auch gegen offene EAP-Builds (z.B. 261/262),
    // deren Plugin-Layout (u.a. Spring-Boot-Bündelung) sich noch ändert und falsch-positive
    // "package not found"-Fehler erzeugt. Bindung an die getestete 253.x-Branch (2025.3.x).
    pluginConfiguration {
        ideaVersion {
        }
    }

    // com.intellij.spring.boot ist im vom Verifier bezogenen IU-Testimage nicht gebündelt
    // (bestätigt: com.intellij.spring taucht im aufgelösten Dependency-Baum nirgends auf), obwohl
    // es in einer echten IntelliJ-Ultimate-Installation vorhanden ist. Der Zugriff darauf ist
    // ueber withSpringBoot.xml bereits korrekt optional gated - ohne diesen Hinweis meldet der
    // Verifier trotzdem "No such class" fuer SpringBootApplicationRunConfiguration.
    pluginVerification {
        ides {
                // Verifier testet gezielt nur gegen diese eine Version, statt gegen den offenen
                // (evtl. EAP-)Bereich, der aus since-build/until-build abgeleitet würde
                create(IntelliJPlatformType.IntellijIdeaUltimate, "2025.3.5")
            }
            externalPrefixes = listOf("com.intellij.spring")
        }
}

intellijPlatformTesting {
    runIde {
        register("runIdeTestJavascript") {
            task {
                args = listOf("C:/Users/Abuscom/workspace/test-javascript")
            }
        }
    }
}

intellijPlatformTesting {
    runIde {
        register("runIdeTest-Maven-springboot") {
            task {
                args = listOf("C:/Users/Abuscom/workspace/maven-test")
            }
        }
    }
}

intellijPlatformTesting {
    runIde {
        register("runISA") {
            task {
                args = listOf("C:/Users/Abuscom/workspace/isa")
            }
        }
    }
}


intellijPlatformTesting {
    runIde {
        register("runIDX") {
            task {
                args = listOf("C:/Users/Abuscom/workspace/idashx/backend")
            }
        }
    }
}

intellijPlatformTesting {
    runIde {
        register("gradleTEST") {
            task {
                args = listOf("C:/Users/Abuscom/workspace/infisical-test-app")
            }
        }
    }
}

intellijPlatformTesting {
    runIde {
        register("af-workspace") {
            task {
                args = listOf("C:/Users/Abuscom/workspace/af-workspace")
            }
        }
    }
}

intellijPlatformTesting {
    runIde {
        register("apache maven") {
            task {
                args = listOf("C:/Users/Abuscom/workspace/maven-test")
            }
        }
    }
}

