/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - introduce DataFlowStatusMessage
 *
 */

package org.eclipse.dataplane;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlowPrepareMessage;
import org.eclipse.dataplane.domain.registration.ControlPlaneRegistrationMessage;
import org.eclipse.dataplane.port.exception.DataFlowNotifyControlPlaneFailed;
import org.eclipse.dataplane.port.exception.DataplaneNotRegistered;
import org.eclipse.dataplane.port.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.and;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataplane.domain.dataflow.DataFlow.State.COMPLETED;
import static org.eclipse.dataplane.domain.dataflow.DataFlow.State.TERMINATED;

class DataplaneTest {

    private final WireMockServer controlPlane = new WireMockServer(options().port(12313));

    @BeforeEach
    void setUp() {
        controlPlane.start();
    }

    @AfterEach
    void tearDown() {
        controlPlane.stop();
    }

    @Nested
    class NotifyCompleted {

        @Test
        void shouldFail_whenDataFlowDoesNotExist() {
            var dataplane = Dataplane.newInstance().build();

            var result = dataplane.notifyCompleted("dataFlowId");

            assertThat(result.failed()).isTrue();
            assertThatThrownBy(result::orElseThrow).isExactlyInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void shouldReturnFailedFuture_whenControlPlaneIsNotAvailable() {
            var dataplane = Dataplane.newInstance().onPrepare(Result::success).build();
            dataplane.registerControlPlane(new ControlPlaneRegistrationMessage("controlplaneId", URI.create("http://localhost/any")));
            dataplane.prepare("controlplaneId", createPrepareMessage());
            controlPlane.stop();

            var result = dataplane.notifyCompleted("dataFlowId");

            assertThat(result.failed()).isTrue();
            assertThatThrownBy(result::orElseThrow).isExactlyInstanceOf(ConnectException.class);
        }

        @Test
        void shouldReturnFailedFuture_whenControlPlaneRespondWithError() {
            controlPlane.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(500)));

            var dataplane = Dataplane.newInstance().onPrepare(Result::success).build();
            dataplane.registerControlPlane(new ControlPlaneRegistrationMessage("controlplaneId", URI.create("http://localhost/any")));
            dataplane.prepare("controlplaneId", createPrepareMessage());

            var result = dataplane.notifyCompleted("dataFlowId");

            assertThat(result.failed()).isTrue();
            assertThatThrownBy(result::orElseThrow).isExactlyInstanceOf(DataFlowNotifyControlPlaneFailed.class);
            assertThat(dataplane.status("dataFlowId").getContent().state()).isNotEqualTo(COMPLETED.name());
        }

        @Test
        void shouldTransitionToCompleted_whenControlPlaneRespondCorrectly() {
            controlPlane.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)));
            var dataplane = Dataplane.newInstance().onPrepare(Result::success).build();
            dataplane.registerControlPlane(new ControlPlaneRegistrationMessage("controlplaneId", URI.create("http://localhost/any")));
            dataplane.prepare("controlplaneId", createPrepareMessage());

            var result = dataplane.notifyCompleted("dataFlowId");

            assertThat(result.succeeded()).isTrue();
            assertThat(dataplane.status("dataFlowId").getContent().state()).isEqualTo(COMPLETED.name());
        }
    }

    @Nested
    class NotifyErrored {
        @Test
        void shouldFail_whenDataFlowDoesNotExist() {
            var dataplane = Dataplane.newInstance().build();

            var result = dataplane.notifyErrored("dataFlowId", new RuntimeException("some-error"));

            assertThat(result.failed()).isTrue();
            assertThatThrownBy(result::orElseThrow).isExactlyInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void shouldSendDataFlowStatusMessage_whenDataFlowIsErrored() {
            controlPlane.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)));
            var dataplane = Dataplane.newInstance().id("dataplane-id").onPrepare(Result::success).build();
            dataplane.registerControlPlane(new ControlPlaneRegistrationMessage("controlplaneId", URI.create("http://localhost/any")));
            dataplane.prepare("controlplaneId", createPrepareMessage());

            var result = dataplane.notifyErrored("dataFlowId", new RuntimeException("some-error"));

            assertThat(result.succeeded()).isTrue();
            assertThat(dataplane.status("dataFlowId").getContent().state()).isEqualTo(TERMINATED.name());

            controlPlane.verify(postRequestedFor(urlPathEqualTo("/transfers/dataFlowId/dataflow/errored"))
                    .withRequestBody(and(
                            matchingJsonPath("dataplaneId", equalTo("dataplane-id")),
                            matchingJsonPath("dataFlowId", equalTo("dataFlowId")),
                            matchingJsonPath("state", equalTo("TERMINATED")),
                            matchingJsonPath("dataAddress", absent()),
                            matchingJsonPath("error", equalTo("some-error"))
                    ))
            );
        }
    }

    @Nested
    class RegisterDataplane {

        @Test
        void shouldRegisterOnTheControlPlane() {
            controlPlane.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)));

            var dataplane = Dataplane.newInstance()
                    .id("dataplane-id")
                    .endpoint(URI.create("http://localhost/dataplane"))
                    .transferType("SupportedTransferType-PUSH")
                    .label("label-one").label("label-two")
                    .build();

            var result = dataplane.registerOn(controlPlane.baseUrl());

            assertThat(result.succeeded()).isTrue();
            controlPlane.verify(postRequestedFor(urlPathEqualTo("/dataplanes/register"))
                    .withRequestBody(and(
                            matchingJsonPath("dataplaneId", equalTo("dataplane-id")),
                            matchingJsonPath("endpoint", equalTo("http://localhost/dataplane")),
                            matchingJsonPath("transferTypes[0]", equalTo("SupportedTransferType-PUSH")),
                            matchingJsonPath("labels.size()", equalTo("2"))
                    ))
            );
        }

        @Test
        void shouldFail_whenStatusIsNot200() {
            controlPlane.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(409)));

            var dataplane = Dataplane.newInstance()
                    .id("dataplane-id")
                    .endpoint(URI.create("http://localhost/dataplane"))
                    .transferType("SupportedTransferType-PUSH")
                    .label("label-one").label("label-two")
                    .build();

            var result = dataplane.registerOn(controlPlane.baseUrl());

            assertThat(result.succeeded()).isFalse();
            assertThatThrownBy(result::orElseThrow).isExactlyInstanceOf(DataplaneNotRegistered.class);
        }
    }

    private DataFlowPrepareMessage createPrepareMessage() {
        return MessageFactory.createPrepareMessage("dataFlowId", URI.create(controlPlane.baseUrl()), "Something-PUSH");
    }
}
