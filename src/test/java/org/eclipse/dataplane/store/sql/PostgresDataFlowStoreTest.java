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

package org.eclipse.dataplane.store.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataplane.port.store.DataFlowStore;
import org.eclipse.dataplane.port.store.sql.PostgresqlDataFlowStatements;
import org.eclipse.dataplane.port.store.sql.SqlDataFlowStore;
import org.eclipse.dataplane.store.DataFlowStoreTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Testcontainers
class PostgresDataFlowStoreTest extends DataFlowStoreTestBase {

    private static final String POSTGRES_IMAGE = "postgres:18.3";
    private static final String DATABASE = "dataplane";
    private static final String USERNAME = "user";
    private static final String PASSWORD = "password";

    private final ObjectMapper mapper = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    private SqlDataFlowStore store;

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName(DATABASE)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)
            .withInitScript("sql/data_flow_schema.sql");

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
        store = new SqlDataFlowStore(mapper, postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword(), new PostgresqlDataFlowStatements());
    }

    @Override
    protected DataFlowStore store() {
        return store;
    }
}
