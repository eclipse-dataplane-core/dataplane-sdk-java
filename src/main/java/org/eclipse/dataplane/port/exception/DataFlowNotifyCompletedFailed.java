package org.eclipse.dataplane.port.exception;

import java.net.http.HttpResponse;

public class DataFlowNotifyCompletedFailed extends Exception {
    private final HttpResponse<Void> response;

    public DataFlowNotifyCompletedFailed(HttpResponse<Void> response) {
        super("control-plane responded with %s".formatted(response.statusCode()));
        this.response = response;
    }

    public HttpResponse<Void> getResponse() {
        return response;
    }
}
