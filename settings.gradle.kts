rootProject.name = "dataplane-sdk"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":core")
include(":web-jersey")

include(":tests")
