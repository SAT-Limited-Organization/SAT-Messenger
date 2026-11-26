pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
  id("com.gradle.develocity") version "4.6.1"
}

develocity {
  buildScan {
    val isCi = providers.environmentVariable("CI").isPresent
    termsOfUseUrl.set("https://gradle.com/terms-of-service")
    termsOfUseAgree.set("yes")
    uploadInBackground.set(!isCi)
    tag(if (isCi) "CI" else "Local")
    obfuscation {
      ipAddresses { addresses -> addresses.map { "0.0.0.0" } }
    }
    if (isCi) {
      publishing.onlyIf { true }
    }
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "SAT Messenger"

include(":app")
