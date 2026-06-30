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
 * Contains the logic for when the prepare endpoint is called for a data flow. This endpoint is
 * only called on the consumer side. The prepare request signals to the consumer dataplane to
 * initialize a data flow and any resources required for data transfer.
 */
public interface OnPrepare {

    /**
     * Performs the logic when a prepare request is received. For a PUSH transfer, the consumer
     * dataplane must return a DataAddress providing endpoint information for the provider to
     * push the data to.
     *
     * Preparation of the data flow may happen asynchronously, e.g. for long-running provision tasks.
     * If the preparation should happen asynchronously, set the DataFlow's state to
     * {@link DataFlow.State#PREPARING} before returning. Once preparation is completed, use the
     * {@link org.eclipse.dataplane.Dataplane#notifyPrepared(String, OnPrepare)} to signal that
     * preparation is complete. The Dataplane will then call this method again so that the
     * DataAddress can be set on the DataFlow before continuing.
     *
     * @param dataFlow the data flow
     * @return a successful or failed {@link Result}, indicating whether the action was successful;
     *         in case of a failed result, it should provide an exception with error details
     */
    Result<DataFlow> action(DataFlow dataFlow);
}
