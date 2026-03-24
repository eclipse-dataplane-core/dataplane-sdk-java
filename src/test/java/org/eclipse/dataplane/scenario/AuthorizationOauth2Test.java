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

package org.eclipse.dataplane.scenario;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataplane.ControlPlane;
import org.eclipse.dataplane.Dataplane;
import org.eclipse.dataplane.HttpServer;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlowPrepareMessage;
import org.eclipse.dataplane.domain.dataflow.DataFlowResponseMessage;
import org.eclipse.dataplane.domain.registration.AuthorizationProfile;
import org.eclipse.dataplane.domain.registration.ControlPlaneRegistrationMessage;
import org.eclipse.dataplane.domain.registration.Oauth2ClientCredentialsAuthorization;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

public class AuthorizationOauth2Test {

    private final HttpServer httpServer = new HttpServer();
    private final ControlPlane controlPlane = new ControlPlane();
    private final Dataplane dataPlane = Dataplane.newInstance()
            .id("data-plane")
            .registerAuthorization(new Oauth2ClientCredentialsAuthorization())
            .onPrepare(dataFlow -> {
                dataFlow.transitionToPreparing();
                return Result.success(dataFlow);
            })
            .build();

    private final String clientId = UUID.randomUUID().toString();
    private final String clientSecret = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        httpServer.start();
        controlPlane.initialize(httpServer, "/data-plane", "/data-plane");

        httpServer.deploy("/data-plane", dataPlane.controller());
        httpServer.deploy("/oauth2", new Oauth2TokenController(clientId, clientSecret));
    }

    @AfterEach
    void tearDown() {
        httpServer.stop();
    }

    @Test
    void shouldCommunicateWithControlPlaneUsingOauth2Authorization() {
        var controlplaneId = clientId;

        controlPlane.setAuthorizationValidation(requestContext -> requestContext
                        .containsHeaderString("Authorization", authorization -> isValidBearerTokenWithSub(authorization, controlplaneId)));

        var controlPlaneRegistrationMessage = new ControlPlaneRegistrationMessage(
                controlplaneId,
                controlPlane.consumerCallbackAddress(),
                List.of(new AuthorizationProfile("oauth2_client_credentials")
                        .withAttribute("tokenEndpoint", "http://localhost:" + httpServer.port() + "/oauth2/token")
                        .withAttribute("clientId", clientId)
                        .withAttribute("clientSecret", clientSecret))
        );
        dataPlane.registerControlPlane(controlPlaneRegistrationMessage).orElseThrow(RuntimeException::new);

        var transferType = "FileSystemAsync-PUSH";
        var processId = UUID.randomUUID().toString();
        var consumerProcessId = "consumer_" + processId;
        var prepareMessage = createPrepareMessage(consumerProcessId, controlPlane.consumerCallbackAddress(), transferType);

        controlPlane.consumerPrepare(prepareMessage).statusCode(202).extract().as(DataFlowResponseMessage.class);

        var notifyPreparedResult = dataPlane.getById(consumerProcessId)
                .compose(dataFlow -> dataPlane.notifyPrepared(consumerProcessId, Result::success));

        assertThat(notifyPreparedResult.succeeded()).isTrue();
    }

    private boolean isValidBearerTokenWithSub(String authorization, String controlplaneId) {
        var bearer = "Bearer ";
        var isBearer = authorization.startsWith(bearer);
        if (!isBearer) {
            return false;
        }

        try {
            var jwt = SignedJWT.parse(authorization.substring(bearer.length()));
            var sub = jwt.getJWTClaimsSet().getClaims().get("sub");
            return sub.equals(controlplaneId);
        } catch (ParseException e) {
            return false;
        }
    }

    private @NonNull DataFlowPrepareMessage createPrepareMessage(String consumerProcessId, URI callbackAddress, String transferType) {
        return new DataFlowPrepareMessage("theMessageId", "theParticipantId", "theCounterPartyId",
                "theDataspaceContext", consumerProcessId, "theAgreementId", "theDatasetId", callbackAddress,
                transferType, emptyList(), emptyMap());
    }

    @Path("/")
    public static class Oauth2TokenController {

        private final String clientId;
        private final String clientSecret;

        public Oauth2TokenController(String clientId, String clientSecret) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
        }

        @POST
        @Path("/token")
        @Consumes(APPLICATION_FORM_URLENCODED)
        @Produces(APPLICATION_JSON)
        public Response token(
                @FormParam("grant_type") String grantType,
                @FormParam("client_id") String clientId,
                @FormParam("client_secret") String clientSecret
        ) {
            if (!Objects.equals(clientId, this.clientId) || !Objects.equals(clientSecret, this.clientSecret) || !Objects.equals(grantType, "client_credentials")) {
                return Response.status(401).build();
            }

            var token = issueJwt(clientId);
            var responseBody = Map.of("access_token", token);
            return Response.ok(responseBody).build();
        }

        public String issueJwt(String sub) {
            var now = new Date();

            var claimsSet = new JWTClaimsSet.Builder()
                    .subject(sub)
                    .issuer("https://your-app.com")
                    .audience("https://api.your-app.com")
                    .expirationTime(new Date(now.getTime() + 1000))
                    .notBeforeTime(now)
                    .issueTime(now)
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            var header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                    .type(JOSEObjectType.JWT)
                    .build();

            var signedJwt = new SignedJWT(header, claimsSet);

            var secret = "random-256-bit-secret-" + UUID.randomUUID();
            try {
                var signer = new MACSigner(secret.getBytes());
                signedJwt.sign(signer);
            } catch (JOSEException e) {
                throw new RuntimeException(e);
            }

            return signedJwt.serialize();
        }

    }

}
