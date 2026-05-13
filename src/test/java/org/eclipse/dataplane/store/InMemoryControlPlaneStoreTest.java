/*
 *  Copyright (c) 2026 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

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
