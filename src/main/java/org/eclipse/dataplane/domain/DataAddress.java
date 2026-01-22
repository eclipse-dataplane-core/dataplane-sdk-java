package org.eclipse.dataplane.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DataAddress(
        @JsonProperty("@type") String type,
        String endpointType,
        String endpoint,
        List<EndpointProperty> endpointProperties
) {

    public DataAddress(String endpointType, String endpoint, List<EndpointProperty> endpointProperties) {
        this("DataAddress", endpointType, endpoint, endpointProperties);
    }

    public record EndpointProperty (
        String type,
        String name,
        String value
    ) {

    }
}
