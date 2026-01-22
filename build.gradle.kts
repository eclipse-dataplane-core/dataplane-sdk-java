plugins {
    java
    `java-library`
    `maven-publish`
    signing
    id("com.vanniktech.maven.publish") version "0.36.0"
}

group = "org.eclipse.dataplane-core"
version = "0.0.3-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:4.0.0")

    testImplementation(platform("org.junit:junit-bom:6.0.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.rest-assured:rest-assured:6.0.0")
    testImplementation("org.assertj:assertj-core:3.27.6")
    testImplementation("org.awaitility:awaitility:4.3.0")
    testImplementation("org.eclipse.jetty.ee10:jetty-ee10-servlet:12.1.5")
    testImplementation("org.eclipse.jetty:jetty-server:12.1.5")
    val jerseyVersion = "4.0.0"
    testImplementation("org.glassfish.jersey.containers:jersey-container-servlet:${jerseyVersion}")
    testImplementation("org.glassfish.jersey.inject:jersey-hk2:${jerseyVersion}")
    testImplementation("org.glassfish.jersey.media:jersey-media-json-jackson:${jerseyVersion}")
    testImplementation("org.mockito:mockito-core:5.21.0")
    testImplementation("org.slf4j:slf4j-simple:2.0.17")
    testImplementation("org.wiremock:wiremock-jetty12:3.13.2")
}

tasks.test {
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
            connection.set("scm:git:git@github.com:eclipse-dataplane-core/dataplane-sdk-go.git")
        }
    }
}
