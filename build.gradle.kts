plugins {
    id("java")
}

group = "org.eclipse.dataplane"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:4.0.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")
    testImplementation("io.rest-assured:rest-assured:5.5.6")
    testImplementation("org.assertj:assertj-core:3.27.6")
    testImplementation("org.awaitility:awaitility:4.3.0")
    testImplementation("org.eclipse.jetty.ee10:jetty-ee10-servlet:12.1.2")
    testImplementation("org.eclipse.jetty:jetty-server:12.1.2")
    testImplementation("org.glassfish.jersey.containers:jersey-container-servlet:3.1.11")
    testImplementation("org.glassfish.jersey.inject:jersey-hk2:3.1.11")
    testImplementation("org.glassfish.jersey.media:jersey-media-json-jackson:3.1.11")
    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation("org.slf4j:slf4j-simple:2.0.17")
    testImplementation("org.wiremock:wiremock-jetty12:3.13.1")
}

tasks.test {
    useJUnitPlatform()
}
