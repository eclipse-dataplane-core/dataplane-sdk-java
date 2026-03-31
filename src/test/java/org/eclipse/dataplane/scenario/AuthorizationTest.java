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
import org.eclipse.dataplane.authorization.TestAuthorization;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlowPrepareMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStatusMessage;
import org.eclipse.dataplane.domain.registration.ControlPlaneRegistrationMessage;
import org.eclipse.dataplane.port.exception.DataFlowNotifyControlPlaneFailed;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataplane.authorization.TestAuthorization.TOKEN_GENERATOR;

public class AuthorizationTest {

    private final HttpServer httpServer = new HttpServer();
    private final TestAuthorization authorization = new TestAuthorization();
    private final ControlPlane controlPlane = ControlPlane.newInstance()
            .authorizationTokenGenerator(() -> TOKEN_GENERATOR.apply("control-plane-id"))
            .build();
    private final Dataplane dataPlane = Dataplane.newInstance()
            .id("data-plane-id")
            .registerAuthorization(authorization)
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
        controlPlane.setAuthorizationValidation(requestContext ->
                requestContext.containsHeaderString("Authorization", a -> a.startsWith("data-plane-id")));
        var controlPlaneRegistrationMessage = new ControlPlaneRegistrationMessage(
                "control-plane-id",
                controlPlane.consumerCallbackAddress(),
                TestAuthorization.createAuthorizationProfile("data-plane-id")
        );
        dataPlane.registerControlPlane(controlPlaneRegistrationMessage).orElseThrow(RuntimeException::new);

        var consumerProcessId = "consumer_" + UUID.randomUUID();
        var prepareMessage = createPrepareMessage(consumerProcessId, controlPlane.consumerCallbackAddress(), "FileSystemAsync-PUSH");

        controlPlane.consumerPrepare(prepareMessage).statusCode(202).extract().as(DataFlowStatusMessage.class);

        var notifyPreparedResult = dataPlane.getById(consumerProcessId)
                .compose(dataFlow -> dataPlane.notifyPrepared(consumerProcessId, Result::success));

        assertThat(notifyPreparedResult.succeeded()).isTrue();
    }

    @Test
    void shouldGetUnauthorized_whenControlPlaneIsNotAuthenticated() {
        controlPlane.setAuthorizationValidation(requestContext ->
                requestContext.containsHeaderString("Authorization", a -> a.startsWith("data-plane-id")));
        var controlPlaneRegistrationMessage = new ControlPlaneRegistrationMessage(
                "unmatching-control-plane-id",
                controlPlane.consumerCallbackAddress(),
                TestAuthorization.createAuthorizationProfile("data-plane-id")
        );
        dataPlane.registerControlPlane(controlPlaneRegistrationMessage).orElseThrow(RuntimeException::new);

        var consumerProcessId = "consumer_" + UUID.randomUUID();
        var prepareMessage = createPrepareMessage(consumerProcessId, controlPlane.consumerCallbackAddress(), "FileSystemAsync-PUSH");

        controlPlane.consumerPrepare(prepareMessage).statusCode(401);
    }

    @Test
    void shouldGetUnauthorized_withDataPlaneIsNotAuthenticated() {
        controlPlane.setAuthorizationValidation(requestContext ->
                requestContext.containsHeaderString("Authorization", a -> a.startsWith("unmatching-data-plane-id")));
        var controlPlaneRegistrationMessage = new ControlPlaneRegistrationMessage(
                "control-plane-id",
                controlPlane.consumerCallbackAddress(),
                TestAuthorization.createAuthorizationProfile("data-plane-id")
        );
        dataPlane.registerControlPlane(controlPlaneRegistrationMessage).orElseThrow(RuntimeException::new);

        var consumerProcessId = "consumer_" + UUID.randomUUID();
        var prepareMessage = createPrepareMessage(consumerProcessId, controlPlane.consumerCallbackAddress(), "FileSystemAsync-PUSH");

        controlPlane.consumerPrepare(prepareMessage).statusCode(202).extract().as(DataFlowStatusMessage.class);

        var notifyPreparedResult = dataPlane.getById(consumerProcessId)
                .compose(dataFlow -> dataPlane.notifyPrepared(consumerProcessId, Result::success));

        assertThat(notifyPreparedResult.failed()).isTrue();
        assertThat(notifyPreparedResult.getException()).isInstanceOfSatisfying(DataFlowNotifyControlPlaneFailed.class, e -> {
            assertThat(e.getResponse().statusCode()).isEqualTo(401);
        });
    }

    private @NonNull DataFlowPrepareMessage createPrepareMessage(String consumerProcessId, URI callbackAddress, String transferType) {
        return new DataFlowPrepareMessage("theMessageId", "theParticipantId", "theCounterPartyId",
                "theDataspaceContext", consumerProcessId, "theAgreementId", "theDatasetId", callbackAddress,
                transferType, emptyList(), emptyMap());
    }

}
