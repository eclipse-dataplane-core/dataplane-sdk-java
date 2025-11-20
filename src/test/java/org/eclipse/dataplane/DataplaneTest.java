package org.eclipse.dataplane;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlowPrepareMessage;
import org.eclipse.dataplane.port.exception.DataFlowNotFoundException;
import org.eclipse.dataplane.port.exception.DataFlowNotifyCompletedFailed;
import org.eclipse.dataplane.port.exception.DataplaneNotRegistered;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.and;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataplane.domain.dataflow.DataFlow.State.COMPLETED;

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
            assertThatThrownBy(result::orElseThrow).isExactlyInstanceOf(DataFlowNotFoundException.class);
        }

        @Test
        void shouldReturnFailedFuture_whenControlPlaneIsNotAvailable() {
            var dataplane = Dataplane.newInstance().onPrepare(Result::success).build();
            dataplane.prepare(createPrepareMessage());
            controlPlane.stop();

            var result = dataplane.notifyCompleted("dataFlowId");

            assertThat(result.failed()).isTrue();
            assertThatThrownBy(result::orElseThrow).isExactlyInstanceOf(ConnectException.class);
        }

        @Test
        void shouldReturnFailedFuture_whenControlPlaneRespondWithError() {
            controlPlane.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(500)));

            var dataplane = Dataplane.newInstance().onPrepare(Result::success).build();
            dataplane.prepare(createPrepareMessage());

            var result = dataplane.notifyCompleted("dataFlowId");

            assertThat(result.failed()).isTrue();
            assertThatThrownBy(result::orElseThrow).isExactlyInstanceOf(DataFlowNotifyCompletedFailed.class);
            assertThat(dataplane.status("dataFlowId").getContent().state()).isNotEqualTo(COMPLETED.name());
        }

        @Test
        void shouldTransitionToCompleted_whenControlPlaneRespondCorrectly() {
            controlPlane.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)));
            var dataplane = Dataplane.newInstance().onPrepare(Result::success).build();
            dataplane.prepare(createPrepareMessage());

            var result = dataplane.notifyCompleted("dataFlowId");

            assertThat(result.succeeded()).isTrue();
            assertThat(dataplane.status("dataFlowId").getContent().state()).isEqualTo(COMPLETED.name());
        }

        private DataFlowPrepareMessage createPrepareMessage() {
            return new DataFlowPrepareMessage("any", "any", "any", "any", "dataFlowId", "any", "any",
                    controlPlane.baseUrl(), "Something-PUSH", emptyList(), emptyMap());
        }

    }

    @Nested
    class RegisterDataplane {

        @Test
        void shouldRegisterOnTheControlPlane() {
            controlPlane.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)));

            var dataplane = Dataplane.newInstance()
                    .id("dataplane-id")
                    .name("dataplane-name")
                    .description("dataplane-description")
                    .endpoint("http://localhost/dataplane")
                    .transferType("SupportedTransferType-PUSH")
                    .label("label-one").label("label-two")
                    .build();

            var result = dataplane.registerOn(controlPlane.baseUrl());

            assertThat(result.succeeded()).isTrue();
            controlPlane.verify(postRequestedFor(urlPathEqualTo("/dataplanes/register"))
                    .withRequestBody(and(
                            matchingJsonPath("dataplaneId", equalTo("dataplane-id")),
                            matchingJsonPath("name", equalTo("dataplane-name")),
                            matchingJsonPath("description", equalTo("dataplane-description")),
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
                    .name("dataplane-name")
                    .description("dataplane-description")
                    .endpoint("http://localhost/dataplane")
                    .transferType("SupportedTransferType-PUSH")
                    .label("label-one").label("label-two")
                    .build();

            var result = dataplane.registerOn(controlPlane.baseUrl());

            assertThat(result.succeeded()).isFalse();
            assertThatThrownBy(result::orElseThrow).isExactlyInstanceOf(DataplaneNotRegistered.class);
        }
    }
}
