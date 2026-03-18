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

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;

public class Oauth2ClientCredentialsAuthorization implements Authorization {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String type() {
        return "oauth2_client_credentials";
    }

    @Override
    public HttpRequest.Builder apply(HttpRequest.Builder requestBuilder, AuthorizationProfile profile) {
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
            return requestBuilder.header(AUTHORIZATION, "Bearer " + accessToken);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
