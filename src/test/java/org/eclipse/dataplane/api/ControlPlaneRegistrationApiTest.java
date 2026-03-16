/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.dataplane.api;

import io.restassured.http.ContentType;
import org.eclipse.dataplane.Dataplane;
import org.eclipse.dataplane.HttpServer;
import org.eclipse.dataplane.domain.registration.Authorization;
import org.eclipse.dataplane.domain.registration.AuthorizationProfile;
import org.eclipse.dataplane.domain.registration.ControlPlaneRegistrationMessage;
import org.eclipse.dataplane.port.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.http.HttpRequest;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class ControlPlaneRegistrationApiTest {

    private final HttpServer httpServer = new HttpServer();
    private final Dataplane sdk = Dataplane.newInstance()
            .id("consumer")
            .registerAuthorization(new TestAuthorization())
            .build();

    @BeforeEach
    void setUp() {
        httpServer.start();

        httpServer.deploy("/runtime/data-plane", sdk.registrationController());
    }

    @AfterEach
    void tearDown() {
        httpServer.stop();
    }

    @Nested
    class Register {
        @Test
        void shouldRegisterControlPlane() {
            var controlPlaneId = UUID.randomUUID().toString();
            var controlPlaneRegistrationMessage = new ControlPlaneRegistrationMessage(controlPlaneId, "http://something");

            given()
                    .contentType(ContentType.JSON)
                    .basePath("/runtime/data-plane")
                    .port(httpServer.port())
                    .body(controlPlaneRegistrationMessage)
                    .put("/v1/controlplanes")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            var result = sdk.controlPlaneStore().findById(controlPlaneId);

            assertThat(result.succeeded());
            assertThat(result.getContent().getEndpoint()).isEqualTo("http://something");
        }

        @Test
        void shouldReplaceControlPlane_whenSecondCall() {
            var controlPlaneId = UUID.randomUUID().toString();
            var controlPlaneRegistrationMessage = new ControlPlaneRegistrationMessage(controlPlaneId, "http://something");

            given()
                    .contentType(ContentType.JSON)
                    .basePath("/runtime/data-plane")
                    .port(httpServer.port())
                    .body(controlPlaneRegistrationMessage)
                    .put("/v1/controlplanes")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            var updateControlPlane = new ControlPlaneRegistrationMessage(controlPlaneId, "http://new-endpoint");

            given()
                    .contentType(ContentType.JSON)
                    .basePath("/runtime/data-plane")
                    .port(httpServer.port())
                    .body(updateControlPlane)
                    .put("/v1/controlplanes")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            var result = sdk.controlPlaneStore().findById(controlPlaneId);

            assertThat(result.succeeded());
            assertThat(result.getContent().getEndpoint()).isEqualTo("http://new-endpoint");
        }

        @Test
        void shouldReturnBadRequest_whenRequestedAuthMethodNotSupported() {
            var controlPlaneId = UUID.randomUUID().toString();
            var authorization = new AuthorizationProfile("unsupported");
            var controlPlaneRegistrationMessage = new ControlPlaneRegistrationMessage(controlPlaneId, "http://something", List.of(authorization));

            given()
                    .contentType(ContentType.JSON)
                    .basePath("/runtime/data-plane")
                    .port(httpServer.port())
                    .body(controlPlaneRegistrationMessage)
                    .put("/v1/controlplanes")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
        }


        @Test
        void shouldRegisterAuthorizationType() {
            var controlPlaneId = UUID.randomUUID().toString();
            var authorization = new AuthorizationProfile("token");
            var controlPlaneRegistrationMessage = new ControlPlaneRegistrationMessage(controlPlaneId, "http://something", List.of(authorization));

            given()
                    .contentType(ContentType.JSON)
                    .basePath("/runtime/data-plane")
                    .port(httpServer.port())
                    .body(controlPlaneRegistrationMessage)
                    .put("/v1/controlplanes")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);
        }
    }

    @Nested
    class Delete {
        @Test
        void shouldDeleteControlPlane() {
            var controlPlaneId = UUID.randomUUID().toString();
            var controlPlaneRegistrationMessage = new ControlPlaneRegistrationMessage(controlPlaneId, "http://something");

            given()
                    .contentType(ContentType.JSON)
                    .basePath("/runtime/data-plane")
                    .port(httpServer.port())
                    .body(controlPlaneRegistrationMessage)
                    .put("/v1/controlplanes")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            given()
                    .contentType(ContentType.JSON)
                    .basePath("/runtime/data-plane")
                    .port(httpServer.port())
                    .delete("/v1/controlplanes/" + controlPlaneId)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);

            var result = sdk.controlPlaneStore().findById(controlPlaneId);

            assertThat(result.failed());
            assertThat(result.getException()).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void shouldReturn404_whenControlPlaneDoesNotExist() {
            var controlPlaneId = UUID.randomUUID().toString();

            given()
                    .contentType(ContentType.JSON)
                    .basePath("/runtime/data-plane")
                    .port(httpServer.port())
                    .delete("/v1/controlplanes/" + controlPlaneId)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404);
        }
    }

    private static class TestAuthorization implements Authorization {

        @Override
        public String type() {
            return "token";
        }

        @Override
        public HttpRequest.Builder apply(HttpRequest.Builder requestBuilder, AuthorizationProfile profile) {
            return requestBuilder.header("Authorization", profile.stringAttribute("token"));
        }
    }

}
