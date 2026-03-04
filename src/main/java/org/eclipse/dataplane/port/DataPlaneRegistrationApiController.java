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
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataplane.Dataplane;
import org.eclipse.dataplane.domain.registration.ControlPlaneRegistrationMessage;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/v1/controlplanes")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class DataPlaneRegistrationApiController {

    private final Dataplane dataplane;

    public DataPlaneRegistrationApiController(Dataplane dataplane) {
        this.dataplane = dataplane;
    }

    @PUT
    @Path("/")
    public Response register(ControlPlaneRegistrationMessage message) {
        dataplane.registerControlPlane(message).orElseThrow(ExceptionMapper.MAP_TO_WSRS);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        dataplane.deleteControlPlane(id).orElseThrow(ExceptionMapper.MAP_TO_WSRS);
        return Response.noContent().build();
    }

}
