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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.controlplane.ControlPlane;
import org.eclipse.dataplane.domain.registration.AuthorizationProfile;
import org.eclipse.dataplane.port.exception.PersistenceException;
import org.eclipse.dataplane.port.exception.ResourceNotFoundException;
import org.eclipse.dataplane.port.store.ControlPlaneStore;

import java.net.URI;

import static java.lang.String.format;

public class PostgresControlPlaneStore extends AbstractSqlStore implements ControlPlaneStore {

    public PostgresControlPlaneStore(ObjectMapper objectMapper, String databaseUrl, String databaseUsername, String databasePassword) {
        super(objectMapper, databaseUrl, databaseUsername, databasePassword);
    }

    @Override
    public Result<Void> save(ControlPlane controlPlane) {
        var connection = getConnection();

        try (var statement = connection.prepareStatement(upsertControlPlaneTemplate())) {
            statement.setString(1, controlPlane.getId());
            statement.setString(2, controlPlane.getEndpoint().toString());
            statement.setString(3, toJson(controlPlane.getAuthorization()));

            statement.executeUpdate();
            return Result.success();
        } catch (Exception e) {
            return Result.failure(new PersistenceException(String.format("Failed to persist ControlPlane with id %s.", controlPlane.getId()), e));
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public Result<ControlPlane> findById(String controlplaneId) {
        var connection = getConnection();

        try (var statement = connection.prepareStatement(findControlPlaneByIdTemplate())) {
            statement.setString(1, controlplaneId);
            var resultSet = statement.executeQuery();

            if (!resultSet.next()) {
                return Result.failure(new ResourceNotFoundException(format("ControlPlane with id %s not found.", controlplaneId)));
            }

            var controlplane = ControlPlane.newInstance()
                    .id(controlplaneId)
                    .endpoint(URI.create(resultSet.getString("endpoint")))
                    .authorization(fromJson(resultSet.getString("auth"), AuthorizationProfile.class))
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

        try (var statement = connection.prepareStatement(deleteControlPlaneByIdTemplate())) {
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

        try (var statement = connection.prepareStatement(countControlPlaneByIdTemplate())) {
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

    private String upsertControlPlaneTemplate() {
        return "INSERT INTO control_planes (id, endpoint, auth) VALUES (?, ?, ?::json)" +
                " ON CONFLICT (id) DO UPDATE SET" +
                " endpoint = EXCLUDED.endpoint," +
                " auth = EXCLUDED.auth";
    }

    private String findControlPlaneByIdTemplate() {
        return "SELECT * FROM control_planes WHERE id = ?";
    }

    private String deleteControlPlaneByIdTemplate() {
        return "DELETE FROM control_planes WHERE id = ?";
    }

    private String countControlPlaneByIdTemplate() {
        return "SELECT COUNT(*) FROM control_planes WHERE id = ?";
    }
}
