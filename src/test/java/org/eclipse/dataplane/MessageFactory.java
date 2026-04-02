/*
 *  Copyright (c) 2026 Think-it GmbH
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

package org.eclipse.dataplane;

import org.eclipse.dataplane.domain.DataAddress;
import org.eclipse.dataplane.domain.dataflow.DataFlowPrepareMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartMessage;
import org.jspecify.annotations.NonNull;

import java.net.URI;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public interface MessageFactory {

    static @NonNull DataFlowPrepareMessage createPrepareMessage(String consumerProcessId, URI callbackAddress, String transferType) {
        return new DataFlowPrepareMessage("theMessageId", "theParticipantId", "theCounterPartyId",
                "theDataspaceContext", consumerProcessId, "theAgreementId", "theDatasetId", callbackAddress,
                transferType, emptyMap(), emptyList(), emptyMap());
    }

    static @NonNull DataFlowStartMessage createStartMessage(String providerProcessId, URI callbackAddress, String transferType) {
        return createStartMessage(providerProcessId, callbackAddress, transferType, null);
    }

    static @NonNull DataFlowStartMessage createStartMessage(String providerProcessId, URI callbackAddress, String transferType, DataAddress destinationDataAddress) {
        return new DataFlowStartMessage("theMessageId", "theParticipantId", "theCounterPartyId",
                "theDataspaceContext", providerProcessId, "theAgreementId", "theDatasetId", callbackAddress,
                transferType, destinationDataAddress, emptyMap(), emptyList(), emptyMap());
    }
}
