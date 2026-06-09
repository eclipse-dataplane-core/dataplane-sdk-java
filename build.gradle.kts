plugins {
    java
    `java-library`
    `maven-publish`
    signing
    checkstyle
    id("com.vanniktech.maven.publish") version "0.36.0"
}

subprojects {
    apply(plugin = "signing")
    apply(plugin = "maven-publish")
    apply(plugin = "com.vanniktech.maven.publish")

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    signing {
        useGpgCmd()
        sign(publishing.publications)
    }

    mavenPublishing {
        publishToMavenCentral(true)

        signAllPublications()

        pom {
            name.set(project.name)
            description.set("Dataplane Signaling SDK library")
            url.set("https://github.com/eclipse-dataplane-core/dataplane-sdk-java")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    name = "Dataplane Core Dev"
                    email = "dataplane-core-dev@eclipse.org"
                }
            }
            scm {
                url.set("https://github.com/eclipse-dataplane-core/dataplane-sdk-java")
                connection.set("scm:git:git@github.com:eclipse-dataplane-core/dataplane-sdk-java.git")
            }
        }
    }
}
