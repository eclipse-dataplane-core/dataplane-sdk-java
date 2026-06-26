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
 * Contains the logic for when the terminate endpoint is called for a DataFlow. The terminate
 * request signals to the dataplane to terminate a data flow. The dataplane must close/remove
 * resource for the data flow so that data transmission is stopped.
 */
public interface OnTerminate {

    /**
     * Performs the logic when a terminated request is received.
     *
     * @param dataFlow the data flow
     * @return a successful or failed {@link Result}, indicating whether the action was successful;
     *         in case of a failed result, it should provide an exception with error details
     */
    Result<DataFlow> action(DataFlow dataFlow);

}
