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

package org.eclipse.dataplane.authorization;

import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.registration.Authorization;
import org.eclipse.dataplane.domain.registration.AuthorizationProfile;
import org.jspecify.annotations.NonNull;

import java.util.UUID;
import java.util.function.Function;

public class TestAuthorization implements Authorization {

    public static final Function<String, Result<String>> TOKEN_GENERATOR = id -> Result.success(id + "::" + UUID.randomUUID());

    public static @NonNull AuthorizationProfile createAuthorizationProfile(String callerId) {
        var authorizationProfile = new AuthorizationProfile("token");
        authorizationProfile.setAttribute("token", callerId + "::" + UUID.randomUUID());
        return authorizationProfile;
    }

    @Override
    public String type() {
        return "token";
    }

    @Override
    public Result<String> authorizationHeader(AuthorizationProfile profile) {
        return Result.success(profile.stringAttribute("token"));
    }

    @Override
    public Result<String> extractCallerId(String authorizationHeader) {
        return Result.success(authorizationHeader.split("::")[0]);
    }
}
