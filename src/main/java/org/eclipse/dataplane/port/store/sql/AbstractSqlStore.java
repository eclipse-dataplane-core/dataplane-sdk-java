package org.eclipse.dataplane.port.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataplane.port.exception.PersistenceException;

import java.sql.Connection;
import java.sql.DriverManager;

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
}
