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

package org.eclipse.dataplane.scenario;

import org.eclipse.dataplane.ControlPlane;
import org.eclipse.dataplane.Dataplane;
import org.eclipse.dataplane.HttpServer;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlowPrepareMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowResponseMessage;
import org.eclipse.dataplane.domain.registration.Authorization;
import org.eclipse.dataplane.domain.registration.AuthorizationProfile;
import org.eclipse.dataplane.domain.registration.ControlPlaneRegistrationMessage;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpRequest;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

public class AuthorizationTest {

    private final HttpServer httpServer = new HttpServer();
    private final ControlPlane controlPlane = new ControlPlane();
    private final Dataplane dataPlane = Dataplane.newInstance()
            .id("data-plane")
            .registerAuthorization(new TestAuthorization())
            .onPrepare(dataFlow -> {
                dataFlow.transitionToPreparing();
                return Result.success(dataFlow);
            })
            .build();

    @BeforeEach
    void setUp() {
        httpServer.start();
        controlPlane.initialize(httpServer, "/data-plane", "/data-plane");

        httpServer.deploy("/data-plane", dataPlane.controller());
    }

    @AfterEach
    void tearDown() {
        httpServer.stop();
    }

    @Test
    void shouldCommunicateWithControlPlaneUsingRegisteredAuthorization() {
        var authorizationToken = UUID.randomUUID().toString();
        controlPlane.setAuthorizationValidation(requestContext ->
                requestContext.containsHeaderString("Authorization", a -> a.equals(authorizationToken)));
        var authorizationProfile = new AuthorizationProfile("test-authorization");
        authorizationProfile.setAttribute("token", authorizationToken);
        var controlPlaneRegistrationMessage = new ControlPlaneRegistrationMessage(
                UUID.randomUUID().toString(),
                controlPlane.consumerCallbackAddress(),
                List.of(authorizationProfile)
        );
        dataPlane.registerControlPlane(controlPlaneRegistrationMessage).orElseThrow(RuntimeException::new);

        var transferType = "FileSystemAsync-PUSH";
        var processId = UUID.randomUUID().toString();
        var consumerProcessId = "consumer_" + processId;
        var prepareMessage = createPrepareMessage(consumerProcessId, controlPlane.consumerCallbackAddress(), transferType);

        controlPlane.consumerPrepare(prepareMessage).statusCode(202).extract().as(DataFlowResponseMessage.class);

        var notifyPreparedResult = dataPlane.getById(consumerProcessId)
                .compose(dataFlow -> dataPlane.notifyPrepared(consumerProcessId, Result::success));

        assertThat(notifyPreparedResult.succeeded()).isTrue();
    }

    private @NonNull DataFlowPrepareMessage createPrepareMessage(String consumerProcessId, String callbackAddress, String transferType) {
        return new DataFlowPrepareMessage("theMessageId", "theParticipantId", "theCounterPartyId",
                "theDataspaceContext", consumerProcessId, "theAgreementId", "theDatasetId", callbackAddress,
                transferType, emptyList(), emptyMap());
    }

    private static class TestAuthorization implements Authorization {

        @Override
        public String type() {
            return "test-authorization";
        }

        @Override
        public HttpRequest.Builder apply(HttpRequest.Builder requestBuilder, AuthorizationProfile profile) {
            return requestBuilder.header("Authorization", profile.stringAttribute("token"));
        }
    }

}
