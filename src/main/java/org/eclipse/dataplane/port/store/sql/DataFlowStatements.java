package org.eclipse.dataplane.port.store.sql;

public interface DataFlowStatements {

    String upsertTemplate();

    String findByIdTemplate();

}
