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
    implementation(libs.jackson.databind)
    implementation(libs.nimbus.jwt)

    compileOnly(libs.jakarta.rsApi)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
    testImplementation(libs.restAssured)
    testImplementation(libs.assertJ)
    testImplementation(libs.awaitility)

    testImplementation(libs.mockito.core)
    testImplementation(libs.slf4j.simple)

    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.postgresql)
}
