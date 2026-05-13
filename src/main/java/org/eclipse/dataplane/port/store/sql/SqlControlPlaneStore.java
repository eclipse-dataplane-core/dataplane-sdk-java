package org.eclipse.dataplane.port.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.controlplane.ControlPlane;
import org.eclipse.dataplane.domain.registration.AuthorizationProfile;
import org.eclipse.dataplane.port.exception.PersistenceException;
import org.eclipse.dataplane.port.exception.ResourceNotFoundException;
import org.eclipse.dataplane.port.store.ControlPlaneStore;

import java.net.URI;

import static java.lang.String.format;

public class SqlControlPlaneStore extends AbstractSqlStore implements ControlPlaneStore {

    private final ControlPlaneStatements statements;

    public SqlControlPlaneStore(ObjectMapper objectMapper, String databaseUrl, String databaseUsername, String databasePassword, ControlPlaneStatements statements) {
        super(objectMapper, databaseUrl, databaseUsername, databasePassword);
        this.statements = statements;
    }

    @Override
    public Result<Void> save(ControlPlane controlPlane) {
        var connection = getConnection();

        try (var statement = connection.prepareStatement(statements.upsertTemplate())) {
            statement.setString(1, controlPlane.getId());
            statement.setString(2, controlPlane.getEndpoint().toString());
            statement.setString(3, objectMapper.writeValueAsString(controlPlane.getAuthorization()));

            statement.executeUpdate();
            return Result.success();
        } catch (Exception e) {
            return Result.failure(new PersistenceException(format("Failed to persist ControlPlane with id %s.", controlPlane.getId()), e));
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public Result<ControlPlane> findById(String controlplaneId) {
        var connection = getConnection();

        try (var statement = connection.prepareStatement(statements.findByIdTemplate())) {
            statement.setString(1, controlplaneId);
            var resultSet = statement.executeQuery();

            if (!resultSet.next()) {
                return Result.failure(new ResourceNotFoundException(format("ControlPlane with id %s not found.", controlplaneId)));
            }

            var controlplane = ControlPlane.newInstance()
                    .id(controlplaneId)
                    .endpoint(URI.create(resultSet.getString("endpoint")))
                    .authorization(objectMapper.readValue(resultSet.getString("auth"), AuthorizationProfile.class))
                    .build();
            return Result.success(controlplane);
        } catch (Exception e) {
            return Result.failure(new PersistenceException(format("Failed to read ControlPlane with id %s.", controlplaneId), e));
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public Result<Void> delete(String id) {
        var connection = getConnection();

        try (var statement = connection.prepareStatement(statements.deleteByIdTemplate())) {
            statement.setString(1, id);
            var rows = statement.executeUpdate();
            if (rows < 1) {
                return Result.failure(new ResourceNotFoundException(format("ControlPlane with id %s not found.", id)));
            }
            return Result.success();
        } catch (Exception e) {
            return Result.failure(new PersistenceException(format("Failed to delete ControlPlane with id %s.", id), e));
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public boolean exists(String controlplaneId) {
        var connection = getConnection();

        try (var statement = connection.prepareStatement(statements.countByIdTemplate())) {
            statement.setString(1, controlplaneId);
            var resultSet = statement.executeQuery();
            resultSet.next();
            return resultSet.getInt(1) > 0;
        } catch (Exception e) {
            throw new PersistenceException(format("Failed to check for existence of ControlPlane with id %s.", controlplaneId), e);
        } finally {
            closeConnection(connection);
        }
    }

}
