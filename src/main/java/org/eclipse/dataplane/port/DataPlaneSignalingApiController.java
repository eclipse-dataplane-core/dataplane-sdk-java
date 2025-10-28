package org.eclipse.dataplane.port;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataplane.Dataplane;
import org.eclipse.dataplane.domain.dataflow.DataFlow;
import org.eclipse.dataplane.domain.dataflow.DataFlowPrepareMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartedNotificationMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStatusResponseMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowTerminateMessage;
import org.eclipse.dataplane.port.exception.DataFlowNotFoundException;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.WILDCARD;

@Path("/v1/dataflows")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class DataPlaneSignalingApiController {

    private final Dataplane dataplane;

    public DataPlaneSignalingApiController(Dataplane dataplane) {
        this.dataplane = dataplane;
    }

    @POST
    @Path("/prepare")
    public Response prepare(DataFlowPrepareMessage message) {
        var response = dataplane.prepare(message).orElseThrow(this::mapToWsRsException);
        if (response.state().equals(DataFlow.State.PREPARING.name())) {
            return Response.accepted(response).build();
        }
        return Response.ok(response).build();
    }

    @POST
    @Path("/start")
    public Response start(DataFlowStartMessage message) {
        var response = dataplane.start(message).orElseThrow(this::mapToWsRsException);
        if (response.state().equals(DataFlow.State.STARTING.name())) {
            return Response.accepted(response).build();
        }
        return Response.ok(response).build();
    }

    @POST
    @Path("/{flowId}/terminate")
    public Response terminate(@PathParam("flowId") String flowId, DataFlowTerminateMessage message) {
        dataplane.terminate(flowId, message).orElseThrow(this::mapToWsRsException);
        return Response.ok().build();
    }

    @POST
    @Path("/{flowId}/started")
    public Response started(@PathParam("flowId") String flowId, DataFlowStartedNotificationMessage startedNotificationMessage) {
        dataplane.started(flowId, startedNotificationMessage).orElseThrow(this::mapToWsRsException);
        return Response.ok().build();
    }

    @POST
    @Path("/{flowId}/completed")
    @Consumes(WILDCARD)
    public Response completed(@PathParam("flowId") String flowId) {
        dataplane.completed(flowId).orElseThrow(this::mapToWsRsException);
        return Response.ok().build();
    }

    @GET
    @Path("/{flowId}/status")
    public DataFlowStatusResponseMessage status(@PathParam("flowId") String flowId) {
        return dataplane.status(flowId).orElseThrow(this::mapToWsRsException);
    }

    private WebApplicationException mapToWsRsException(Exception exception) {
        if (exception instanceof DataFlowNotFoundException notFound) {
            return new NotFoundException(notFound);
        }
        return new WebApplicationException("unexpected internal server error");
    }

}
