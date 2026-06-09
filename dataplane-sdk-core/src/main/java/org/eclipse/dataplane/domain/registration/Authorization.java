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

package org.eclipse.dataplane.domain.registration;

import org.eclipse.dataplane.domain.Result;

/**
 * Defines structure for an authorization profile.
 */
public interface Authorization {

    /**
     * Return the authorization profile type string
     */
    String type();

    /**
     * Function that applies the authorization profile to the request builder.
     * e.g. the Authorization header could be added with proper content.
     */
    Result<String> authorizationHeader(AuthorizationProfile profile);

    Result<String> extractCallerId(String authorizationHeader);
}
