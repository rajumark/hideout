// Standalone consumer of the PUBLISHED Hideout artifacts, used to check a release before and after
// it goes live. It never sees the library source: io.github.rajumark comes only from
//   Maven Local   (default):          ./gradlew ...
//   Maven Central (-PhideoutRepo=central): ./gradlew ... -PhideoutRepo=central
rootProject.name = "hideout-sample"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val fromCentral = providers.gradleProperty("hideoutRepo").orNull == "central"

dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository { if (fromCentral) mavenCentral() else mavenLocal() }
            filter { includeGroup("io.github.rajumark") }
        }
        google()
        mavenCentral()
    }
}

include(":shared")
include(":androidApp")
include(":desktopApp")
include(":webApp")
