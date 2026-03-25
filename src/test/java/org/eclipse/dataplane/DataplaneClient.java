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

package org.eclipse.dataplane;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlowPrepareMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartedNotificationMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowSuspendMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowTerminateMessage;

import java.util.function.Supplier;

import static io.restassured.RestAssured.given;

public class DataplaneClient {

    private final String baseUri;
    private final Supplier<Result<String>> authorizationTokenGenerator;

    public DataplaneClient(String baseUri, Supplier<Result<String>> authorizationTokenGenerator) {
        this.baseUri = baseUri;
        this.authorizationTokenGenerator = authorizationTokenGenerator;
    }

    public ValidatableResponse prepare(DataFlowPrepareMessage prepareMessage) {
        return baseRequest()
                .body(prepareMessage)
                .post("/v1/dataflows/prepare")
                .then()
                .log().ifValidationFails();
    }

    public ValidatableResponse start(DataFlowStartMessage startMessage) {
        return baseRequest()
                .body(startMessage)
                .post("/v1/dataflows/start")
                .then()
                .log().ifValidationFails();
    }

    public ValidatableResponse terminate(String dataFlowId, DataFlowTerminateMessage terminateMessage) {
        return baseRequest()
                .body(terminateMessage)
                .post("/v1/dataflows/{id}/terminate", dataFlowId)
                .then()
                .log().ifValidationFails();
    }

    public ValidatableResponse status(String dataFlowId) {
        return given()
                .baseUri(baseUri)
                .get("/v1/dataflows/{id}/status", dataFlowId)
                .then()
                .log().ifValidationFails();
    }

    public ValidatableResponse started(String dataFlowId, DataFlowStartedNotificationMessage startedNotificationMessage) {
        return baseRequest()
                .body(startedNotificationMessage)
                .post("/v1/dataflows/{id}/started", dataFlowId)
                .then()
                .log().ifValidationFails();
    }

    public ValidatableResponse completed(String dataFlowId) {
        return baseRequest()
                .post("/v1/dataflows/{id}/completed", dataFlowId)
                .then()
                .log().ifValidationFails();
    }

    public ValidatableResponse suspend(String flowId, DataFlowSuspendMessage suspendMessage) {
        return baseRequest()
                .body(suspendMessage)
                .post("/v1/dataflows/{id}/suspend", flowId)
                .then()
                .log().ifValidationFails();
    }

    private RequestSpecification baseRequest() {
        var requestSpecification = given()
                .contentType(ContentType.JSON)
                .baseUri(baseUri);

        if (authorizationTokenGenerator != null) {
            authorizationTokenGenerator.get()
                    .onSuccess(authorizationHeader -> requestSpecification.header("Authorization", authorizationHeader));
        }

        return requestSpecification;
    }

}
