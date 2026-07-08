rootProject.name = "dataplane-sdk"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

include(":dataplane-sdk-core")
include(":dataplane-sdk-jakarta-ee")
include(":dataplane-sdk-postgresql")

include(":e2e-tests")
