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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - Javadoc
 *
 */

package org.eclipse.dataplane.port.store;

import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlow;

/**
 * Store for {@link DataFlow}s.
 */
public interface DataFlowStore {

    /**
     * Persists the given DataFlow.
     *
     * @param dataFlow the DataFlow to persist
     * @return a successful or failed {@link Result}, indicating whether the DataFlow was persisted;
     *         in case of a failed result, it should provide an exception with error details
     */
    Result<Void> save(DataFlow dataFlow);

    /**
     * Retrieves a stored DataFlow by id.
     *
     * @param flowId the id of the DataFlow
     * @return a successful {@link Result} holding the DataFlow, or a failed result with an
     *         exception providing error details
     */
    Result<DataFlow> findById(String flowId);
}
