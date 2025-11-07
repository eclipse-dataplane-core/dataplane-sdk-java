package org.eclipse.dataplane.domain.dataflow;

import java.util.List;
import java.util.Map;

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
        List<String> labels,
        Map<String, Object> metadata
) {
}
