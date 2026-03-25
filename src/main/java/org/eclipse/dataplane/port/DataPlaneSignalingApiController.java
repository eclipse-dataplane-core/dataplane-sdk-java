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

package org.eclipse.dataplane.port;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataplane.Dataplane;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlow;
import org.eclipse.dataplane.domain.dataflow.DataFlowPrepareMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStartedNotificationMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowStatusResponseMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowSuspendMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowTerminateMessage;
import org.eclipse.dataplane.domain.registration.Authorization;

import java.util.Map;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.WILDCARD;

@Path("/v1/dataflows")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class DataPlaneSignalingApiController {

    private final Dataplane dataplane;
    private final Map<String, Authorization> authorizations;

    public DataPlaneSignalingApiController(Dataplane dataplane, Map<String, Authorization> authorizations) {
        this.dataplane = dataplane;
        this.authorizations = authorizations;
    }

    @POST
    @Path("/prepare")
    public Response prepare(DataFlowPrepareMessage message, @Context ContainerRequestContext requestContext) {
        var response = extractControlplaneId(requestContext)
                .compose(controlplaneId -> dataplane.prepare(controlplaneId, message))
                .orElseThrow(ExceptionMapper.MAP_TO_WSRS);

        if (response.state().equals(DataFlow.State.PREPARING.name())) {
            return Response.accepted(response).build();
        }
        return Response.ok(response).build();
    }

    @POST
    @Path("/start")
    public Response start(DataFlowStartMessage message, @Context ContainerRequestContext requestContext) {
        var response = extractControlplaneId(requestContext)
                .compose(controlplaneId -> dataplane.start(controlplaneId, message))
                .orElseThrow(ExceptionMapper.MAP_TO_WSRS);

        if (response.state().equals(DataFlow.State.STARTING.name())) {
            return Response.accepted(response).build();
        }
        return Response.ok(response).build();
    }

    @POST
    @Path("/{flowId}/suspend")
    public Response suspend(@PathParam("flowId") String flowId, DataFlowSuspendMessage message) {
        dataplane.suspend(flowId, message).orElseThrow(ExceptionMapper.MAP_TO_WSRS);
        return Response.ok().build();
    }

    @POST
    @Path("/{flowId}/terminate")
    public Response terminate(@PathParam("flowId") String flowId, DataFlowTerminateMessage message) {
        dataplane.terminate(flowId, message).orElseThrow(ExceptionMapper.MAP_TO_WSRS);
        return Response.ok().build();
    }

    @POST
    @Path("/{flowId}/started")
    public Response started(@PathParam("flowId") String flowId, DataFlowStartedNotificationMessage startedNotificationMessage) {
        dataplane.started(flowId, startedNotificationMessage).orElseThrow(ExceptionMapper.MAP_TO_WSRS);
        return Response.ok().build();
    }

    @POST
    @Path("/{flowId}/completed")
    @Consumes(WILDCARD)
    public Response completed(@PathParam("flowId") String flowId) {
        dataplane.completed(flowId).orElseThrow(ExceptionMapper.MAP_TO_WSRS);
        return Response.ok().build();
    }

    @GET
    @Path("/{flowId}/status")
    public DataFlowStatusResponseMessage status(@PathParam("flowId") String flowId) {
        return dataplane.status(flowId).orElseThrow(ExceptionMapper.MAP_TO_WSRS);
    }

    private Result<String> extractControlplaneId(ContainerRequestContext requestContext) {
        var authorizationHeader = requestContext.getHeaderString("Authorization");
        if (authorizationHeader == null) {
            return Result.failure(new NotAuthorizedException("Authorization header missing"));
        }
        return authorizations.values().stream()
                .map(authorization -> authorization.extractCallerId(authorizationHeader))
                .filter(Result::succeeded).findFirst()
                .orElseGet(() -> Result.failure(new NotAuthorizedException("Authorization method not recognized")));
    }

}
