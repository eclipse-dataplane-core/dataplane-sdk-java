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

public class PostgresqlDataFlowStatements implements DataFlowStatements {
    @Override
    public String upsertTemplate() {
        return "INSERT INTO data_flows (id, transfer_type, type, state, dataset_id, agreement_id, participant_id,"
                + " counter_party_id, dataspace_context, callback_address, suspension_reason, termination_reason,"
                + " labels, metadata, data_address, controlplane_id) VALUES"
                + " (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::json, ?::json, ?::json, ?)"
                + " ON CONFLICT (id) DO UPDATE SET"
                + " transfer_type = EXCLUDED.transfer_type,"
                + " type = EXCLUDED.type,"
                + " state = EXCLUDED.state,"
                + " dataset_id = EXCLUDED.dataset_id,"
                + " agreement_id = EXCLUDED.agreement_id,"
                + " participant_id = EXCLUDED.participant_id,"
                + " counter_party_id = EXCLUDED.counter_party_id,"
                + " dataspace_context = EXCLUDED.dataspace_context,"
                + " callback_address = EXCLUDED.callback_address,"
                + " suspension_reason = EXCLUDED.suspension_reason,"
                + " termination_reason = EXCLUDED.termination_reason,"
                + " labels = EXCLUDED.labels,"
                + " metadata = EXCLUDED.metadata,"
                + " data_address = EXCLUDED.data_address,"
                + " controlplane_id = EXCLUDED.controlplane_id";
    }

    @Override
    public String findByIdTemplate() {
        return "SELECT * FROM data_flows WHERE id = ?";
    }
}
