package org.eclipse.dataplane.port.store;

import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.controlplane.ControlPlane;
import org.eclipse.dataplane.domain.registration.AuthorizationProfile;
import org.eclipse.dataplane.port.exception.PersistenceException;
import org.eclipse.dataplane.port.exception.ResourceNotFoundException;

import java.net.URI;

import static java.lang.String.format;

public class SqlControlPlaneStore extends AbstractSqlStore implements ControlPlaneStore {

    public SqlControlPlaneStore(String databaseUrl, String databaseUsername, String databasePassword) {
        super(databaseUrl, databaseUsername, databasePassword);
    }

    @Override
    public Result<Void> save(ControlPlane controlPlane) {
        var connection = getConnection();

        var sql = "INSERT INTO control_planes (id, endpoint, authorization) VALUES (?, ?, ?::json)"
                + " ON CONFLICT (id) DO UPDATE SET"
                + " endpoint = EXCLUDED.endpoint,"
                + " authorization = EXCLUDED.authorization";

        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, controlPlane.getId());
            statement.setString(2, controlPlane.getEndpoint().toString());
            statement.setString(3, objectMapper.writeValueAsString(controlPlane.getAuthorization()));

            statement.executeUpdate();
            return Result.success();
        } catch (Exception e) {
            return Result.failure(new PersistenceException(format("Failed to persist ControlPlane with ID %s.", controlPlane.getId()), e));
        }
    }

    @Override
    public Result<ControlPlane> findById(String controlplaneId) {
        var connection = getConnection();

        var sql = "SELECT * FROM control_planes WHERE id = ?";

        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, controlplaneId);
            var resultSet = statement.executeQuery();

            if (!resultSet.next()) {
                return Result.failure(new ResourceNotFoundException(format("ControlPlane with id %s not found.", controlplaneId)));
            }

            var controlplane = ControlPlane.newInstance()
                    .id(controlplaneId)
                    .endpoint(URI.create(resultSet.getString("endpoint")))
                    .authorization(objectMapper.readValue(resultSet.getString("authorization"), AuthorizationProfile.class))
                    .build();
            return Result.success(controlplane);
        } catch (Exception e) {
            return Result.failure(new PersistenceException(format("Failed to read ControlPlane with ID %s.", controlplaneId), e));
        }
    }

    @Override
    public Result<Void> delete(String id) {
        var connection = getConnection();

        var sql = "DELETE FROM control_planes WHERE id = ?";

        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.executeUpdate();
            return Result.success();
        } catch (Exception e) {
            return Result.failure(new PersistenceException(format("Failed to delete ControlPlane with ID %s.", id), e));
        }
    }

    @Override
    public boolean exists(String controlplaneId) {
        var connection = getConnection();

        var sql = "SELECT COUNT(*) FROM control_planes WHERE id = ?";

        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, controlplaneId);
            var resultSet = statement.executeQuery();
            return resultSet.getInt(1) > 0;
        } catch (Exception e) {
            throw new PersistenceException(format("Failed to check for existence of ControlPlane with ID %s.", controlplaneId), e);
        }
    }

}
