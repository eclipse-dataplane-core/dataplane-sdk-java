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

package org.eclipse.dataplane.port.store.sql;

public class PostgresqlControlPlaneStatements implements ControlPlaneStatements {
    @Override
    public String upsertControlPlaneTemplate() {
        return "INSERT INTO control_planes (id, endpoint, auth) VALUES (?, ?, ?::json)"
                + " ON CONFLICT (id) DO UPDATE SET"
                + " endpoint = EXCLUDED.endpoint,"
                + " auth = EXCLUDED.auth";
    }

    @Override
    public String findControlPlaneByIdTemplate() {
        return "SELECT * FROM control_planes WHERE id = ?";
    }

    @Override
    public String deleteControlPlaneByIdTemplate() {
        return "DELETE FROM control_planes WHERE id = ?";
    }

    @Override
    public String countControlPlaneByIdTemplate() {
        return "SELECT COUNT(*) FROM control_planes WHERE id = ?";
    }
}
