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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - introduce DataFlowStatusMessage
 *
 */

package org.eclipse.dataplane.domain.dataflow;

import org.eclipse.dataplane.domain.DataAddress;

import java.util.UUID;

public record DataFlowStatusMessage(
        String messageId,
        String dataFlowId,
        String state,
        DataAddress dataAddress,
        String error
) {
    public DataFlowStatusMessage(String dataFlowId, String state, DataAddress dataAddress, String error) {
        this(UUID.randomUUID().toString(), dataFlowId, state, dataAddress, error);
    }
}
