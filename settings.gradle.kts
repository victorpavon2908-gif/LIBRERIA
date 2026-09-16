pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "LibreriaProUltra"
include(":app")

// Lets Gradle download the JDK pinned in gradle/gradle-daemon-jvm.properties
// (Java 21) when it is not installed yet. See that file for the reason.
plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

