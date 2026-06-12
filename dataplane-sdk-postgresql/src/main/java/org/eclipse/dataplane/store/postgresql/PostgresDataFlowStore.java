/*
 *  Copyright (c) 2026 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.dataplane.store.postgresql;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataplane.domain.DataAddress;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlow;
import org.eclipse.dataplane.port.exception.PersistenceException;
import org.eclipse.dataplane.port.exception.ResourceNotFoundException;
import org.eclipse.dataplane.port.store.DataFlowStore;

import java.net.URI;

import static java.lang.String.format;

public class PostgresDataFlowStore extends AbstractSqlStore implements DataFlowStore {

    public PostgresDataFlowStore(ObjectMapper objectMapper, String databaseUrl, String databaseUsername, String databasePassword) {
        super(objectMapper, databaseUrl, databaseUsername, databasePassword);
    }

    @Override
    public Result<Void> save(DataFlow dataFlow) {
        var connection = getConnection();

        try (var statement = connection.prepareStatement(upsertDataFlowTemplate())) {
            statement.setString(1, dataFlow.getId());
            statement.setString(2, dataFlow.getTransferType());
            statement.setString(3, dataFlow.getType().name());
            statement.setString(4, dataFlow.getState().name());
            statement.setString(5, dataFlow.getDatasetId());
            statement.setString(6, dataFlow.getAgreementId());
            statement.setString(7, dataFlow.getParticipantId());
            statement.setString(8, dataFlow.getCounterPartyId());
            statement.setString(9, dataFlow.getDataspaceContext());
            statement.setString(10, dataFlow.getCallbackAddress().toString());
            statement.setString(11, dataFlow.getSuspensionReason());
            statement.setString(12, dataFlow.getTerminationReason());
            statement.setString(13, toJson(dataFlow.getLabels()));
            statement.setString(14, toJson(dataFlow.getMetadata()));
            statement.setString(15, toJson(dataFlow.getDataAddress()));
            statement.setString(16, dataFlow.getControlplaneId());

            statement.executeUpdate();
            return Result.success();
        } catch (Exception e) {
            return Result.failure(new PersistenceException(String.format("Failed to persist DataFlow with id %s.", dataFlow.getId()), e));
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public Result<DataFlow> findById(String flowId) {
        var connection = getConnection();

        try (var statement = connection.prepareStatement(findDataFlowByIdTemplate())) {
            statement.setString(1, flowId);
            var resultSet = statement.executeQuery();

            if (!resultSet.next()) {
                return Result.failure(new ResourceNotFoundException(format("DataFlow with id %s not found.", flowId)));
            }

            var dataFlow = DataFlow.newInstance()
                    .id(flowId)
                    .state(DataFlow.State.valueOf(resultSet.getString("state")))
                    .transferType(resultSet.getString("transfer_type"))
                    .datasetId(resultSet.getString("dataset_id"))
                    .agreementId(resultSet.getString("agreement_id"))
                    .participantId(resultSet.getString("participant_id"))
                    .counterPartyId(resultSet.getString("counter_party_id"))
                    .dataspaceContext(resultSet.getString("dataspace_context"))
                    .callbackAddress(URI.create(resultSet.getString("callback_address")))
                    .suspensionReason(resultSet.getString("suspension_reason"))
                    .terminationReason(resultSet.getString("termination_reason"))
                    .labels(fromJson(resultSet.getString("labels"), new TypeReference<>() {}))
                    .metadata(fromJson(resultSet.getString("metadata"), new TypeReference<>() {}))
                    .dataAddress(fromJson(resultSet.getString("data_address"), DataAddress.class))
                    .controlplaneId(resultSet.getString("controlplane_id"))
                    .type(DataFlow.Type.valueOf(resultSet.getString("type")))
                    .build();

            return Result.success(dataFlow);
        } catch (Exception e) {
            return Result.failure(new PersistenceException(format("Failed to read DataFlow with id %s.", flowId), e));
        } finally {
            closeConnection(connection);
        }
    }

    private String upsertDataFlowTemplate() {
        return "INSERT INTO data_flows (id, transfer_type, type, state, dataset_id, agreement_id, participant_id," +
                " counter_party_id, dataspace_context, callback_address, suspension_reason, termination_reason," +
                " labels, metadata, data_address, controlplane_id) VALUES" +
                " (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::json, ?::json, ?::json, ?)" +
                " ON CONFLICT (id) DO UPDATE SET" +
                " transfer_type = EXCLUDED.transfer_type," +
                " type = EXCLUDED.type," +
                " state = EXCLUDED.state," +
                " dataset_id = EXCLUDED.dataset_id," +
                " agreement_id = EXCLUDED.agreement_id," +
                " participant_id = EXCLUDED.participant_id," +
                " counter_party_id = EXCLUDED.counter_party_id," +
                " dataspace_context = EXCLUDED.dataspace_context," +
                " callback_address = EXCLUDED.callback_address," +
                " suspension_reason = EXCLUDED.suspension_reason," +
                " termination_reason = EXCLUDED.termination_reason," +
                " labels = EXCLUDED.labels," +
                " metadata = EXCLUDED.metadata," +
                " data_address = EXCLUDED.data_address," +
                " controlplane_id = EXCLUDED.controlplane_id";
    }

    private String findDataFlowByIdTemplate() {
        return "SELECT * FROM data_flows WHERE id = ?";
    }
}
