package org.eclipse.dataplane.port.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataplane.port.exception.PersistenceException;

import java.sql.Connection;
import java.sql.DriverManager;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

public abstract class AbstractSqlStore {

    protected final ObjectMapper objectMapper = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final String databaseUrl;
    private final String databaseUsername;
    private final String databasePassword;

    public AbstractSqlStore(String databaseUrl, String databaseUsername, String databasePassword) {
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

}
