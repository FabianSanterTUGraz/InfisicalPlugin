import org.jetbrains.intellij.platform.gradle.TestFrameworkType

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
                args = listOf("C:/Users/Abuscom/workspace/hard_test/idashx/backend")
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

