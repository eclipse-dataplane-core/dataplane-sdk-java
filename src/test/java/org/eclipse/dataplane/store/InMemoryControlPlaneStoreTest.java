package org.eclipse.dataplane.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataplane.port.store.ControlPlaneStore;
import org.eclipse.dataplane.port.store.InMemoryControlPlaneStore;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

class InMemoryControlPlaneStoreTest extends ControlPlaneStoreTestBase {

    private InMemoryControlPlaneStore store = new InMemoryControlPlaneStore(new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false));

    @Override
    protected ControlPlaneStore store() {
        return store;
    }
}
