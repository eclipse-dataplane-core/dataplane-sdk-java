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

    public record EndpointProperty(
            String type,
            String name,
            String value
    ) {

    }
}
