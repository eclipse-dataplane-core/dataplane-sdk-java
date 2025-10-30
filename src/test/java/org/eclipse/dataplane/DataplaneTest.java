package org.eclipse.dataplane;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlowPrepareMessage;
import org.eclipse.dataplane.port.exception.DataFlowNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataplane.domain.dataflow.DataFlow.State.COMPLETED;

class DataplaneTest {

    @Nested
    class NotifyCompleted {

        private final WireMockServer controlPlane = new WireMockServer(options().port(12313));

        @BeforeEach
        void setUp() {
            controlPlane.start();
        }

        @AfterEach
        void tearDown() {
            controlPlane.stop();
        }

        @Test
        void shouldFail_whenDataFlowDoesNotExist() {
            var dataplane = Dataplane.newInstance().build();

            var result = dataplane.notifyCompleted("dataFlowId");

            assertThat(result.failed());
            assertThatThrownBy(result::orElseThrow).isExactlyInstanceOf(DataFlowNotFoundException.class);
        }

        @Test
        void shouldReturnFailedFuture_whenControlPlaneIsNotAvailable() {
            var dataplane = Dataplane.newInstance().onPrepare(Result::success).build();
            dataplane.prepare(new DataFlowPrepareMessage("any", "any", "any", "any", "dataFlowId", "any", "any", controlPlane.baseUrl(), "Something-PUSH", null));
            controlPlane.stop();

            var result = dataplane.notifyCompleted("dataFlowId");

            assertThat(result.succeeded());
            assertThat(result.getContent()).failsWithin(5, TimeUnit.SECONDS);
        }

        @Test
        void shouldReturnFailedFuture_whenControlPlaneRespondWithError() {
            controlPlane.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(500)));

            var dataplane = Dataplane.newInstance().onPrepare(Result::success).build();
            dataplane.prepare(new DataFlowPrepareMessage("any", "any", "any", "any", "dataFlowId", "any", "any", controlPlane.baseUrl(), "Something-PUSH", null));

            var result = dataplane.notifyCompleted("dataFlowId");

            assertThat(result.succeeded());
            assertThat(result.getContent()).failsWithin(5, TimeUnit.SECONDS);
            assertThat(dataplane.status("dataFlowId").getContent().state()).isNotEqualTo(COMPLETED.name());
        }

        @Test
        void shouldTransitionToCompleted_whenControlPlaneRespondCorrectly() {
            controlPlane.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)));
            var dataplane = Dataplane.newInstance().onPrepare(Result::success).build();
            dataplane.prepare(new DataFlowPrepareMessage("any", "any", "any", "any", "dataFlowId", "any", "any", controlPlane.baseUrl(), "Something-PUSH", null));

            var result = dataplane.notifyCompleted("dataFlowId");

            assertThat(result.succeeded());
            assertThat(result.getContent()).succeedsWithin(5, TimeUnit.SECONDS);
            assertThat(dataplane.status("dataFlowId").getContent().state()).isEqualTo(COMPLETED.name());
        }

        @Test
        void shouldRetryForCertainAmountOfCalls() {
            controlPlane.stubFor(post(anyUrl()).inScenario("retry")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse().withStatus(500))
                    .willSetStateTo("RETRY"));

            controlPlane.stubFor(post(anyUrl()).inScenario("retry")
                    .whenScenarioStateIs("RETRY")
                    .willReturn(aResponse().withStatus(200)));

            var dataplane = Dataplane.newInstance().onPrepare(Result::success).build();
            dataplane.prepare(new DataFlowPrepareMessage("any", "any", "any", "any", "dataFlowId", "any", "any", controlPlane.baseUrl(), "Something-PUSH", null));

            var result = dataplane.notifyCompleted("dataFlowId");

            assertThat(result.succeeded());
            assertThat(result.getContent()).succeedsWithin(5, TimeUnit.SECONDS);
            assertThat(dataplane.status("dataFlowId").getContent().state()).isEqualTo(COMPLETED.name());
        }

        // TODO: retry case

    }
}
