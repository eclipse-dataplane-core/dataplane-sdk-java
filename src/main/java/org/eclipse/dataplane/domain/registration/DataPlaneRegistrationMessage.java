package org.eclipse.dataplane.domain.registration;

import java.util.Set;

public record DataPlaneRegistrationMessage(
        String dataplaneId,
        String name,
        String description,
        String endpoint,
        Set<String> transferTypes,
        Set<String> labels
        // TODO: authorization
) {
}
