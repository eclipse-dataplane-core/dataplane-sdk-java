package org.eclipse.dataplane.domain.dataflow;

import org.eclipse.dataplane.domain.DataAddress;

public record DataFlowPrepareMessage(
        String messageId,
        String participantId,
        String counterPartyId,
        String dataspaceContext,
        String processId,
        String agreementId,
        String datasetId,
        String callbackAddress, // TODO: make URI!
        String transferType,
        DataAddress dataAddress
) {
}
