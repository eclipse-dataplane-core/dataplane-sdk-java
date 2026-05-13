package org.eclipse.dataplane.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataplane.port.store.DataFlowStore;
import org.eclipse.dataplane.port.store.InMemoryDataFlowStore;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

class InMemoryDataFlowStoreTest extends DataFlowStoreTestBase {

    private InMemoryDataFlowStore store = new InMemoryDataFlowStore(new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false));

    @Override
    protected DataFlowStore store() {
        return store;
    }
}
