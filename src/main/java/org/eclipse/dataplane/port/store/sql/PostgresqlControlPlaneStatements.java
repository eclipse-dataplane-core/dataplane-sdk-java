package org.eclipse.dataplane.port.store.sql;

public class PostgresqlControlPlaneStatements implements ControlPlaneStatements {
    @Override
    public String upsertTemplate() {
        return "INSERT INTO control_planes (id, endpoint, auth) VALUES (?, ?, ?::json)"
                + " ON CONFLICT (id) DO UPDATE SET"
                + " endpoint = EXCLUDED.endpoint,"
                + " auth = EXCLUDED.auth";
    }

    @Override
    public String findByIdTemplate() {
        return "SELECT * FROM control_planes WHERE id = ?";
    }

    @Override
    public String deleteByIdTemplate() {
        return "DELETE FROM control_planes WHERE id = ?";
    }

    @Override
    public String countByIdTemplate() {
        return "SELECT COUNT(*) FROM control_planes WHERE id = ?";
    }
}
