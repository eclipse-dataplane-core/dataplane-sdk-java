/*
 *  Copyright (c) 2026 Think-it GmbH
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

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.dataplane.port.exception.AuthorizationNotSupported;
import org.eclipse.dataplane.port.exception.ControlPlaneNotRegistered;
import org.eclipse.dataplane.port.exception.ResourceNotFoundException;

import java.util.function.Function;

public interface ExceptionMapper {

    Function<Exception, WebApplicationException> MAP_TO_WSRS = exception -> {
        if (exception instanceof WebApplicationException webApplicationException) {
            return webApplicationException;
        }

        if (exception instanceof ResourceNotFoundException notFound) {
            return new NotFoundException(notFound);
        }

        if (exception instanceof ControlPlaneNotRegistered controlPlaneNotRegistered) {
            return new NotAuthorizedException(controlPlaneNotRegistered);
        }

        if (exception instanceof AuthorizationNotSupported) {
            return new BadRequestException(exception);
        }

        return new WebApplicationException("unexpected internal server error");
    };

}
