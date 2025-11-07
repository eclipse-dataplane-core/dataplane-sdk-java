package org.eclipse.dataplane.domain.dataflow;

import org.eclipse.dataplane.domain.DataAddress;

import java.util.List;
import java.util.Map;

public record DataFlowStartMessage(
        String messageId,
        String participantId,
        String counterPartyId,
        String dataspaceContext,
        String processId,
        String agreementId,
        String datasetId,
        String callbackAddress,
        String transferType,
        DataAddress dataAddress,
        List<String> labels,
        Map<String, Object> metadata
) {
}
