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

package org.eclipse.dataplane.port.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.controlplane.ControlPlane;
import org.eclipse.dataplane.port.exception.ResourceNotFoundException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class InMemoryControlPlaneStore implements ControlPlaneStore {

    private final ObjectMapper objectMapper;
    private final Map<String, String> store = new HashMap<>();

    public InMemoryControlPlaneStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Result<Void> save(ControlPlane controlPlane) {
        try {
            store.put(controlPlane.getId(), objectMapper.writeValueAsString(controlPlane));
            return Result.success();
        } catch (JsonProcessingException e) {
            return Result.failure(e);
        }
    }

    @Override
    public Result<ControlPlane> findById(String controlplaneId) {
        var json = store.get(controlplaneId);
        if (json == null) {
            return Result.failure(new ResourceNotFoundException("ControlPlane %s not found".formatted(controlplaneId)));
        }

        return deserialize(json);
    }

    @Override
    public Result<Void> delete(String id) {
        var remove = store.remove(id);
        if (remove == null) {
            return Result.failure(new ResourceNotFoundException("ControlPlane %s not found".formatted(id)));
        }
        return Result.success();
    }

    @Override
    public Result<ControlPlane> findByEndpoint(URI endpoint) {
        return store.values().stream().map(this::deserialize).filter(Result::succeeded)
                .map(Result::getContent).filter(it -> Objects.equals(endpoint, it.getEndpoint()))
                .findAny().map(Result::success)
                .orElseGet(() -> Result.failure(new ResourceNotFoundException("ControlPlane with endpoint %s not found".formatted(endpoint))));
    }

    private Result<ControlPlane> deserialize(String json) {
        try {
            var deserialized = objectMapper.readValue(json, ControlPlane.class);
            return Result.success(deserialized);
        } catch (JsonProcessingException e) {
            return Result.failure(e);
        }
    }
}
