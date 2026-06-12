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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataplane.port.exception.PersistenceException;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Base class for SQL-based store implementations that provides methods for common functionality
 * like connection handling and JSON parsing.
 */
public abstract class AbstractSqlStore {

    protected ObjectMapper objectMapper;

    private final String databaseUrl;
    private final String databaseUsername;
    private final String databasePassword;

    public AbstractSqlStore(ObjectMapper objectMapper, String databaseUrl, String databaseUsername, String databasePassword) {
        this.objectMapper = objectMapper;
        this.databaseUrl = databaseUrl;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
    }

    protected Connection getConnection() {
        try {
            return DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword);
        } catch (Exception e) {
            throw new PersistenceException("Failed to connect to database.", e);
        }
    }

    protected void closeConnection(Connection connection) {
        try {
            connection.close();
        } catch (Exception e) {
            throw new PersistenceException("Failed to commit transaction.", e);
        }
    }

    protected String toJson(Object object) {
        if (object == null) {
            return null;
        }

        try {
            return object instanceof String ? object.toString() : objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new PersistenceException("Failed to convert object to JSON.", e);
        }
    }

    protected <T> T fromJson(String json, Class<T> type) {
        return fromJson(json, objectMapper.getTypeFactory().constructType(type));
    }

    protected <T> T fromJson(String json, TypeReference<T> type) {
        return fromJson(json, objectMapper.getTypeFactory().constructType(type));
    }

    protected <T> T fromJson(String json, JavaType type) {
        if (json == null) {
            return null;
        }

        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new PersistenceException("Failed to convert JSON to object.", e);
        }
    }
}
