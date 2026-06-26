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
import org.eclipse.dataplane.domain.controlplane.ControlPlane;

/**
 * Store for {@link ControlPlane}s.
 */
public interface ControlPlaneStore {

    /**
     * Persists the given ControlPlane.
     *
     * @param controlPlane the DataFlow to persist
     * @return a successful or failed {@link Result}, indicating whether the ControlPlane was persisted;
     *         in case of a failed result, it should provide an exception with error details
     */
    Result<Void> save(ControlPlane controlPlane);

    /**
     * Retrieves a stored ControlPlane by id.
     *
     * @param controlplaneId the id of the ControlPlane
     * @return a successful {@link Result} holding the ControlPlane, or a failed result with an
     *         exception providing error details
     */
    Result<ControlPlane> findById(String controlplaneId);

    /**
     * Deletes a stored ControlPlane by id.
     *
     * @param id the id of the ControlPlane
     * @return a successful or failed {@link Result}, indicating whether the ControlPlane was deleted;
     *         in case of a failed result, it should provide an exception with error details
     */
    Result<Void> delete(String id);

    /**
     * Checks for existence of a ControlPlane by id.
     *
     * @param controlplaneId the id of the ControlPlane
     * @return true, if the ControlPlane exists in the store, false otherwise
     */
    boolean exists(String controlplaneId);
}
