package org.eclipse.dataplane.store;

import org.eclipse.dataplane.domain.DataAddress;
import org.eclipse.dataplane.domain.dataflow.DataFlow;
import org.eclipse.dataplane.port.exception.ResourceNotFoundException;
import org.eclipse.dataplane.port.store.DataFlowStore;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class DataFlowStoreTestBase {

    @Nested
    class Save {
        @Test
        void save_newDataFlow_shouldCreate() {
            var id = "id";
            var dataFlow = dataFlow(id);

            var result = store().save(dataFlow);
            assertThat(result.succeeded()).isTrue();

            var persisted = store().findById(id).getContent();
            assertThat(persisted).isNotNull();
            assertThat(persisted).usingRecursiveComparison().isEqualTo(dataFlow);
        }

        @Test
        void save_existingDataFlow_shouldUpdate() {
            var id = "toUpdate";
            var dataFlow = dataFlow(id);
            store().save(dataFlow);
            var persisted = store().findById(id).getContent();
            assertThat(persisted.getState()).isEqualTo(DataFlow.State.INITIATING);
            assertThat(persisted.getSuspensionReason()).isNull();

            var suspensionReason = "suspend";
            dataFlow.transitionToSuspended(suspensionReason);
            store().save(dataFlow);

            var updated = store().findById(id).getContent();
            assertThat(updated.getState()).isEqualTo(DataFlow.State.SUSPENDED);
            assertThat(updated.getSuspensionReason()).isNotNull().isEqualTo(suspensionReason);
        }
    }

    @Nested
    class FindById {
        @Test
        void findById_exists_shouldReturnDataFlow() {
            var id = "id";
            var dataFlow = dataFlow(id);
            store().save(dataFlow);

            var result = store().findById(id);

            assertThat(result.succeeded()).isTrue();
            assertThat(result.getContent()).isNotNull();
        }

        @Test
        void findById_doesNotExist_shouldReturnNotFound() {
            var result = store().findById("nonExistent");

            assertThat(result.failed()).isTrue();
            assertThat(result.getException()).isInstanceOf(ResourceNotFoundException.class);
        }
    }

    protected abstract DataFlowStore store();

    private DataFlow dataFlow(String id) {
        return DataFlow.newInstance()
                .id(id)
                .state(DataFlow.State.INITIATING)
                .transferType("HTTP-PUSH")
                .datasetId("dataset")
                .agreementId("agreement")
                .participantId("participant")
                .counterPartyId("counterParty")
                .dataspaceContext("dataspaceContext")
                .callbackAddress(URI.create("https://callbackAddress"))
                .labels(List.of("label1", "label2"))
                .metadata(Map.of("key1", "value1", "key2", "value2"))
                .dataAddress(new DataAddress("http", "https://endpoint", List.of()))
                .controlplaneId("controlPlane")
                .type(DataFlow.Type.PROVIDER)
                .build();
    }
}
