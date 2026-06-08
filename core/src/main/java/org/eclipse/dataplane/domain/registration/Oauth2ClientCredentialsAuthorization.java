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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataplane.domain.Result;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;

public class Oauth2ClientCredentialsAuthorization implements Authorization {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String type() {
        return "oauth2_client_credentials";
    }

    @Override
    public Result<String> authorizationHeader(AuthorizationProfile profile) {
        var tokenEndpoint = profile.stringAttribute("tokenEndpoint");

        var parameters = Map.of(
                "grant_type", "client_credentials",
                "client_id", profile.stringAttribute("clientId"),
                "client_secret", profile.stringAttribute("clientSecret")
        );

        var form = parameters.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));


        var request = HttpRequest.newBuilder(URI.create(tokenEndpoint))
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .header("Content-Type", APPLICATION_FORM_URLENCODED)
                .build();

        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            var body = response.body();
            var accessToken = objectMapper.readValue(body, Map.class).get("access_token").toString();
            return Result.success("Bearer " + accessToken);
        } catch (Exception e) {
            return Result.failure(e);
        }

    }

    @Override
    public Result<String> extractCallerId(String authorizationHeader) {
        try {
            var token = authorizationHeader.substring("Bearer ".length());
            var jwt = SignedJWT.parse(token);
            var sub = jwt.getJWTClaimsSet().getClaims().get("sub");
            if (sub instanceof String callerId) {
                return Result.success(callerId);
            }
            return Result.failure(new RuntimeException("JWT sub claim %s is not a string".formatted(sub)));
        } catch (Exception e) {
            return Result.failure(e);
        }
    }
}
