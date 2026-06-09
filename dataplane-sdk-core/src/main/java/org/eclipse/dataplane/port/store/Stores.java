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

package org.eclipse.dataplane.port.store;

/**
 * Data class that bundles the stores used by the dataplane.
 *
 * @param dataFlowStore store for data flows
 * @param controlPlaneStore store for control planes
 */
public record Stores(DataFlowStore dataFlowStore, ControlPlaneStore controlPlaneStore) {
}
