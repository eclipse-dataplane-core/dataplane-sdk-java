package org.eclipse.dataplane.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataplane.port.store.ControlPlaneStore;
import org.eclipse.dataplane.port.store.sql.PostgresqlControlPlaneStatements;
import org.eclipse.dataplane.port.store.sql.SqlControlPlaneStore;
import org.eclipse.dataplane.store.ControlPlaneStoreTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Testcontainers
class PostgresControlPlaneStoreTest extends ControlPlaneStoreTestBase {

    private static final String POSTGRES_IMAGE = "postgres:18.3";
    private static final String DATABASE = "dataplane";
    private static final String USERNAME = "user";
    private static final String PASSWORD = "password";

    private final ObjectMapper mapper = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    private SqlControlPlaneStore store;

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName(DATABASE)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)
            .withInitScript("sql/control_plane_schema.sql");

    @BeforeAll
    static void init() {
        postgres.start();
    }

    @AfterAll
    static void cleanUp() {
        postgres.stop();
        postgres.close();
    }

    @BeforeEach
    void initStore() {
        store = new SqlControlPlaneStore(mapper, postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword(), new PostgresqlControlPlaneStatements());
    }

    @Override
    protected ControlPlaneStore store() {
        return store;
    }
}
