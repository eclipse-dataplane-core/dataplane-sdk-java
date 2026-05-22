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

import org.eclipse.dataplane.domain.dataflow.DataFlow;

/**
 * Provides templates for SQL statements for managing data flows. Used by the
 * {@link SqlDataFlowStore} within PreparedStatements. Can be implemented for different SQL
 * dialects.
 */
public interface DataFlowStatements {

    /**
     * Provides the template for an upsert statement for data flows. The returned statement must
     * contain placeholders for all properties of a data flow.
     *
     * @return the upsert statement template including the placeholders
     */
    String upsertDataFlowTemplate();

    /**
     * Provides the template for a find-by-id statement for data flows.The returned statement must
     * contain a placeholder for the data flow id.
     *
     * @return the find-by-id statement template including the placeholder
     */
    String findDataFlowByIdTemplate();

}
