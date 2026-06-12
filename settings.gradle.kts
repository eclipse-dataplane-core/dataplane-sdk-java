rootProject.name = "dataplane-sdk"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":dataplane-sdk-core")
include(":dataplane-sdk-jakarta-ee")
include(":dataplane-sdk-postgresql")

include(":e2e-tests")
