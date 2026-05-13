package org.eclipse.dataplane.port.store.sql;

public interface ControlPlaneStatements {

    String upsertTemplate();

    String findByIdTemplate();

    String deleteByIdTemplate();

    String countByIdTemplate();

}
