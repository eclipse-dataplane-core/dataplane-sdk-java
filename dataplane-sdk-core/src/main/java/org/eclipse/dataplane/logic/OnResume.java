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

package org.eclipse.dataplane.logic;

import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlow;

/**
 * Contains the logic for when the resume endpoint is called for a DataFlow. The resume request
 * signals to the dataplane to resume a suspended data flow and start data transmission again.
 */
public interface OnResume {

    /**
     * Performs the logic when a resume request is received. The dataplane should start transmitting
     * any data for the data flow.
     *
     * For PUSH transfers, a consumer data plane must provide a DataAddress. For PULL transfers,
     * a provider data plane must provide a DataAddress. Either, the original DataAddress may be
     * used, or a new DataAddress may be generated and set on the DataFlow.
     *
     * @param dataFlow the data flow
     * @return a successful or failed {@link Result}, indicating whether the action was successful;
     *         in case of a failed result, it should provide an exception with error details
     */
    Result<DataFlow> action(DataFlow dataFlow);

}
