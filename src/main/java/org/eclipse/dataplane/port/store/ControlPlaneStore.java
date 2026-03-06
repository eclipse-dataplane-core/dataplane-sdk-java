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
 *
 */

package org.eclipse.dataplane.port.store;

import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.controlplane.ControlPlane;

public interface ControlPlaneStore {
    Result<Void> save(ControlPlane controlPlane);

    Result<ControlPlane> findById(String controlplaneId);

    Result<Void> delete(String id);

    Result<ControlPlane> findByEndpoint(String endpoint);
}
