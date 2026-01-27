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

package org.eclipse.dataplane.port.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlow;
import org.eclipse.dataplane.port.exception.DataFlowNotFoundException;

import java.util.HashMap;
import java.util.Map;

public class InMemoryDataFlowStore implements DataFlowStore {

    private final Map<String, String> store = new HashMap<>();
    private final ObjectMapper objectMapper;

    public InMemoryDataFlowStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Result<Void> save(DataFlow dataFlow) {
        try {
            store.put(dataFlow.getId(), objectMapper.writeValueAsString(dataFlow));
            return Result.success();
        } catch (JsonProcessingException e) {
            return Result.failure(e);
        }
    }

    @Override
    public Result<DataFlow> findById(String flowId) {
        var dataFlow = store.get(flowId);
        if (dataFlow == null) {
            return Result.failure(new DataFlowNotFoundException("DataFlow %s not found".formatted(flowId)));
        }

        try {
            var deserialized = objectMapper.readValue(dataFlow, DataFlow.class);
            return Result.success(deserialized);
        } catch (JsonProcessingException e) {
            return Result.failure(e);
        }
    }
}
