rootProject.name = "dataplane-sdk"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":core")
include(":web-jakarta-ee")

include(":e2e-tests")
