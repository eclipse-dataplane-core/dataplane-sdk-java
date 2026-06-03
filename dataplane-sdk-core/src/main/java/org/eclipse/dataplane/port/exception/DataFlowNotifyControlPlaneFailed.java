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
 *
 */

package org.eclipse.dataplane.port.exception;

import java.net.http.HttpResponse;

public class DataFlowNotifyControlPlaneFailed extends Exception {
    private final String action;
    private final HttpResponse<String> response;

    public DataFlowNotifyControlPlaneFailed(String action, HttpResponse<String> response) {
        super("control-plane responded with %s: %s".formatted(response.statusCode(), response.body()));
        this.action = action;
        this.response = response;
    }

    public HttpResponse<String> getResponse() {
        return response;
    }

    public String getAction() {
        return action;
    }
}
