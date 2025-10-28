package org.eclipse.dataplane;

import io.restassured.response.ValidatableResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.dataplane.domain.dataflow.DataFlowPrepareMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartedNotificationMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowTerminateMessage;

import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static jakarta.ws.rs.core.MediaType.WILDCARD;

/**
 * This simulates control plane for both consumer and provider.
 * For the sake of simplicity given that there's no DSP wire layer, the flowIds will be the same on provider and on consumer,
 * just prefixed with either "provider_" and "consumer_"
 */
public class ControlPlane {

    private final DataplaneClient consumerClient;
    private final DataplaneClient providerClient;
    private final HttpServer httpServer;

    public ControlPlane(HttpServer httpServer, String consumerDataPlanePath, String providerDataPlanePath) {
        this.httpServer = httpServer;
        consumerClient = new DataplaneClient("http://localhost:%d%s".formatted(httpServer.port(), consumerDataPlanePath));
        providerClient = new DataplaneClient("http://localhost:%d%s".formatted(httpServer.port(), providerDataPlanePath));

        httpServer.deploy("/consumer/control-plane", new ControlPlaneController(providerClient));
        httpServer.deploy("/provider/control-plane", new ControlPlaneController(consumerClient));

    }

    public ValidatableResponse consumerPrepare(DataFlowPrepareMessage prepareMessage) {
        return consumerClient.prepare(prepareMessage);
    }

    public ValidatableResponse consumerStarted(String dataFlowId, DataFlowStartedNotificationMessage startedNotificationMessage) {
        return consumerClient.started(dataFlowId, startedNotificationMessage);
    }

    public ValidatableResponse providerStart(DataFlowStartMessage startMessage) {
        return providerClient.start(startMessage);
    }

    public ValidatableResponse providerStatus(String flowId) {
        return providerClient.status(flowId);
    }

    public ValidatableResponse consumerStatus(String flowId) {
        return consumerClient.status(flowId);
    }

    public ValidatableResponse providerTerminate(String dataFlowId, DataFlowTerminateMessage terminateMessage) {
        return providerClient.terminate(dataFlowId, terminateMessage);
    }

    public String providerCallbackAddress() {
        return "http://localhost:%d/provider/control-plane".formatted(httpServer.port());
    }

    public String consumerCallbackAddress() {
        return "http://localhost:%d/consumer/control-plane".formatted(httpServer.port());
    }

    @Path("/transfers")
    public static class ControlPlaneController {

        private final ExecutorService executor = Executors.newCachedThreadPool();
        private final DataplaneClient counterPart;

        public ControlPlaneController(DataplaneClient counterPart) {
            this.counterPart = counterPart;
        }

        @POST
        @Path("/{transferId}/dataflow/completed")
        @Consumes(WILDCARD)
        public void completed(@PathParam("transferId") String transferId) {
            executor.submit(() -> {
                var idPart = transferId.split("_")[1];
                counterPart.completed("consumer_" + idPart).statusCode(200);
            });
        }

    }
}
