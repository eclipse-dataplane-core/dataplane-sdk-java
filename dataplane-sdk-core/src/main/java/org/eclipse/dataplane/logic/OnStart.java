/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - Javadoc
 *
 */

package org.eclipse.dataplane.logic;

import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlow;

/**
 * Contains the logic for when the start endpoint is called for a data flow. This endpoint is only
 * called on the provider side. The start request signals to the dataplane to begin a data transfer
 * or to initialize a data flow and any resources required for data transfer.
 */
public interface OnStart {

    /**
     * Performs the logic when a start request is received. For a PUSH transfer, the provider
     * dataplane receives a DataAddress in the start message and should start sending data to
     * the respective endpoint. For a PULL transfer, the provider dataplane must return a
     * DataAddress providing endpoint information for the consumer to pull the data from.
     *
     * Starting of the data flow may happen asynchronously, e.g. for long-running provision tasks.
     * If the start should happen asynchronously, set the DataFlow's state to
     * {@link DataFlow.State#STARTING} before returning. Once start logic is completed, use the
     * {@link org.eclipse.dataplane.Dataplane#notifyStarted(String, OnStart)} to signal that
     * start is complete. The Dataplane will then call this method again so that the
     * DataAddress can be set on the DataFlow before continuing.
     *
     * @param dataFlow the data flow
     * @return a successful or failed {@link Result}, indicating whether the action was successful;
     *         in case of a failed result, it should provide an exception with error details
     */
    Result<DataFlow> action(DataFlow dataFlow);

}
