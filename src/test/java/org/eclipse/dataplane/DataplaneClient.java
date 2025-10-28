package org.eclipse.dataplane;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.eclipse.dataplane.domain.dataflow.DataFlowPrepareMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartedNotificationMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowTerminateMessage;

import static io.restassured.RestAssured.given;

public class DataplaneClient {

    private final String baseUri;

    public DataplaneClient(String baseUri) {
        this.baseUri = baseUri;
    }

    public ValidatableResponse prepare(DataFlowPrepareMessage prepareMessage) {
        return given()
                .contentType(ContentType.JSON)
                .baseUri(baseUri)
                .body(prepareMessage)
                .post("/v1/dataflows/prepare")
                .then()
                .log().ifValidationFails();
    }

    public ValidatableResponse start(DataFlowStartMessage startMessage) {
        return given()
                .contentType(ContentType.JSON)
                .baseUri(baseUri)
                .body(startMessage)
                .post("/v1/dataflows/start")
                .then()
                .log().ifValidationFails();
    }

    public ValidatableResponse terminate(String dataFlowId, DataFlowTerminateMessage terminateMessage) {
        return given()
                .contentType(ContentType.JSON)
                .baseUri(baseUri)
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
        return given()
                .contentType(ContentType.JSON)
                .baseUri(baseUri)
                .body(startedNotificationMessage)
                .post("/v1/dataflows/{id}/started", dataFlowId)
                .then()
                .log().ifValidationFails();
    }

    public ValidatableResponse completed(String dataFlowId) {
        return given()
                .baseUri(baseUri)
                .post("/v1/dataflows/{id}/completed", dataFlowId)
                .then()
                .log().ifValidationFails();
    }
}
