package org.eclipse.dataplane;

import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlowPrepareMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowResponseMessage;
import org.eclipse.dataplane.port.exception.DataFlowNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataplaneTest {

    @Nested
    class NotifyCompleted {

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
            dataplane.prepare(new DataFlowPrepareMessage("any", "any", "any", "any", "dataFlowId", "any", "any", "http://localhost:12313", "Something-PUSH", null));

            var result = dataplane.notifyCompleted("dataFlowId");

            assertThat(result.succeeded());
            assertThat(result.getContent()).failsWithin(5, TimeUnit.SECONDS);
        }

        @Test
        void shouldTransitionToCompleted_whenControlPlaneRespondCorrectly() {
            // TODO: fake control plane
            var dataplane = Dataplane.newInstance().onPrepare(Result::success).build();
            var prepare = dataplane.prepare(new DataFlowPrepareMessage("any", "any", "any", "any", "dataFlowId", "any", "any", "http://localhost:12313", "Something-PUSH", null));

            var result = dataplane.notifyCompleted("dataFlowId");

            assertThat(result.succeeded());
            assertThat(result.getContent()).succeedsWithin(5, TimeUnit.SECONDS);
        }

        // TODO: retry case

    }
}
