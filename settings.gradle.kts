rootProject.name = "dataplane-sdk"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":dataplane-sdk-core")
include(":dataplane-sdk-jakarta-ee")

include(":e2e-tests")
