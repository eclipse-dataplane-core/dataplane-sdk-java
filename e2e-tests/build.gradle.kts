/*
 *  Copyright (c) 2026 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    testImplementation(project(":dataplane-sdk-core"))
    testImplementation(project(":dataplane-sdk-jakarta-ee"))

    testImplementation(libs.nimbus.jwt)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
    testImplementation(libs.restAssured)
    testImplementation(libs.assertJ)
    testImplementation(libs.awaitility)

    testImplementation(libs.jakarta.rsApi)
    testImplementation(libs.jersey.servlet)
    testImplementation(libs.jersey.hk2)
    testImplementation(libs.jersey.jackson)
    testImplementation(libs.jetty.ee10.servlet)
    testImplementation(libs.jetty.server)
    testImplementation(libs.wiremock.jetty12)
}

tasks.withType<PublishToMavenRepository>().configureEach {
    enabled = false
}

tasks.withType<PublishToMavenLocal>().configureEach {
    enabled = false
}
