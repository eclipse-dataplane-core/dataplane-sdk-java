package org.eclipse.dataplane.port.store;

import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.dataplane.domain.DataAddress;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlow;
import org.eclipse.dataplane.port.exception.PersistenceException;
import org.eclipse.dataplane.port.exception.ResourceNotFoundException;

import java.net.URI;

import static java.lang.String.format;

public class SqlDataFlowStore extends AbstractSqlStore implements DataFlowStore {

    public SqlDataFlowStore(String databaseUrl, String databaseUsername, String databasePassword) {
        super(databaseUrl, databaseUsername, databasePassword);
    }

    @Override
    public Result<Void> save(DataFlow dataFlow) {
        var connection = getConnection();

        var sql = "INSERT INTO data_flows (id, transfer_type, type, state, dataset_id, agreement_id, participant_id,"
                + " counter_party_id, dataspace_context, callback_address, suspension_reason, termination_reason,"
                + " labels, metadata, data_address, controlplane_id) VALUES"
                + " (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::json, ?::json, ?::json, ?)"
                + " ON CONFLICT (id) DO UPDATE SET"
                + " transfer_type = EXCLUDED.transfer_type,"
                + " type = EXCLUDED.type,"
                + " state = EXCLUDED.state,"
                + " dataset_id = EXCLUDED.dataset_id,"
                + " agreement_id = EXCLUDED.agreement_id,"
                + " participant_id = EXCLUDED.participant_id,"
                + " counter_party_id = EXCLUDED.counter_party_id,"
                + " dataspace_context = EXCLUDED.dataspace_context,"
                + " callback_address = EXCLUDED.callback_address,"
                + " suspension_reason = EXCLUDED.suspension_reason,"
                + " termination_reason = EXCLUDED.termination_reason,"
                + " labels = EXCLUDED.labels,"
                + " metadata = EXCLUDED.metadata,"
                + " data_address = EXCLUDED.data_address,"
                + " controlplane_id = EXCLUDED.controlplane_id";

        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, dataFlow.getId());
            statement.setString(2, dataFlow.getState().name());
            statement.setString(3, dataFlow.getTransferType());
            statement.setString(4, dataFlow.getDatasetId());
            statement.setString(5, dataFlow.getAgreementId());
            statement.setString(6, dataFlow.getParticipantId());
            statement.setString(7, dataFlow.getCounterPartyId());
            statement.setString(8, dataFlow.getDataspaceContext());
            statement.setString(9, dataFlow.getCallbackAddress().toString());
            statement.setString(10, dataFlow.getSuspensionReason());
            statement.setString(11, dataFlow.getTerminationReason());
            statement.setString(12, objectMapper.writeValueAsString(dataFlow.getLabels()));
            statement.setString(13, objectMapper.writeValueAsString(dataFlow.getMetadata()));
            statement.setString(14, objectMapper.writeValueAsString(dataFlow.getDataAddress()));
            statement.setString(15, dataFlow.getControlplaneId());
            statement.setString(16, dataFlow.getType().name());

            statement.executeUpdate();
            return Result.success();
        } catch (Exception e) {
            return Result.failure(new PersistenceException(format("Failed to persist DataFlow with ID %s.", dataFlow.getId()), e));
        }
    }

    @Override
    public Result<DataFlow> findById(String flowId) {
        var connection = getConnection();

        var sql = "SELECT * FROM data_flows WHERE id = ?";

        try (var statement = connection.prepareStatement(sql)) {
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
                    .labels(objectMapper.readValue(resultSet.getString("labels"), new TypeReference<>() {
                    }))
                    .metadata(objectMapper.readValue(resultSet.getString("metadata"), new TypeReference<>() {
                    }))
                    .dataAddress(objectMapper.readValue(resultSet.getString("data_address"), DataAddress.class))
                    .controlplaneId(resultSet.getString("controlplane_id"))
                    .type(DataFlow.Type.valueOf(resultSet.getString("type")))
                    .build();

            if (dataFlow.getState() == DataFlow.State.SUSPENDED) {
                dataFlow.transitionToSuspended(resultSet.getString("suspension_reason"));
            } else if (dataFlow.getState() == DataFlow.State.TERMINATED) {
                dataFlow.transitionToTerminated(resultSet.getString("termination_reason"));
            }

            return Result.success(dataFlow);
        } catch (Exception e) {
            return Result.failure(new PersistenceException(format("Failed to read DataFlow with ID %s.", flowId), e));
        }
    }
}
