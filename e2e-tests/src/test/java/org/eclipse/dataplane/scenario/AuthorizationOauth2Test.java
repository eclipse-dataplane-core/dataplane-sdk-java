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
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataplane.ControlPlane;
import org.eclipse.dataplane.Dataplane;
import org.eclipse.dataplane.HttpServer;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlowStatusMessage;
import org.eclipse.dataplane.domain.registration.AuthorizationProfile;
import org.eclipse.dataplane.domain.registration.ControlPlaneRegistrationMessage;
import org.eclipse.dataplane.domain.registration.Oauth2ClientCredentialsAuthorization;
import org.eclipse.dataplane.port.DataPlaneSignalingApiController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataplane.MessageFactory.createPrepareMessage;

public class AuthorizationOauth2Test {

    private final HttpServer httpServer = new HttpServer();
    private Oauth2ClientCredentialsAuthorization oauth2ClientCredentialsAuthorization;
    private ControlPlane controlPlane;
    private Dataplane dataPlane;
    private Oauth2TokenController tokenController;

    private final String clientId = UUID.randomUUID().toString();
    private final String clientSecret = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() throws JOSEException {
        httpServer.start();

        tokenController = new Oauth2TokenController(clientId, clientSecret);
        var jwksUri = "http://localhost:" + httpServer.port() + "/oauth2/jwks";
        oauth2ClientCredentialsAuthorization = new Oauth2ClientCredentialsAuthorization(jwksUri);

        dataPlane = Dataplane.newInstance()
                .id("data-plane")
                .registerAuthorization(oauth2ClientCredentialsAuthorization)
                .onPrepare(dataFlow -> {
                    dataFlow.transitionToPreparing();
                    return Result.success(dataFlow);
                })
                .build();

        controlPlane = ControlPlane.newInstance()
                .authorizationTokenGenerator(() -> oauth2ClientCredentialsAuthorization.authorizationHeader(oauth2AuthorizationProfile()))
                .build();

        controlPlane.initialize(httpServer, "/data-plane", "/data-plane");

        httpServer.deploy("/data-plane", new DataPlaneSignalingApiController(dataPlane));
        httpServer.deploy("/oauth2", tokenController);
    }

    @AfterEach
    void tearDown() {
        httpServer.stop();
    }

    @Test
    void shouldCommunicateWithControlPlaneUsingOauth2Authorization() {
        var controlplaneId = clientId;

        controlPlane.setAuthorizationValidation(requestContext -> requestContext
                        .containsHeaderString("Authorization", authorization -> {
                            var callerIdExtraction = oauth2ClientCredentialsAuthorization.extractCallerId(authorization);
                            return callerIdExtraction.succeeded() && Objects.equals(callerIdExtraction.getContent(), controlplaneId);
                        }));

        var controlPlaneRegistrationMessage = new ControlPlaneRegistrationMessage(
                controlplaneId,
                controlPlane.consumerCallbackAddress(),
                oauth2AuthorizationProfile()
        );
        dataPlane.registerControlPlane(controlPlaneRegistrationMessage).orElseThrow(RuntimeException::new);

        var profile = "FileSystemAsync-PUSH";
        var processId = UUID.randomUUID().toString();
        var consumerProcessId = "consumer_" + processId;
        var prepareMessage = createPrepareMessage(consumerProcessId, profile);

        controlPlane.consumerPrepare(prepareMessage).statusCode(202).extract().as(DataFlowStatusMessage.class);

        var notifyPreparedResult = dataPlane.getById(consumerProcessId)
                .compose(dataFlow -> dataPlane.notifyPrepared(consumerProcessId, Result::success));

        assertThat(notifyPreparedResult.succeeded()).isTrue();
    }

    private AuthorizationProfile oauth2AuthorizationProfile() {
        return new AuthorizationProfile("oauth2_client_credentials")
                .withAttribute("tokenEndpoint", "http://localhost:" + httpServer.port() + "/oauth2/token")
                .withAttribute("clientId", clientId)
                .withAttribute("clientSecret", clientSecret);
    }

    @Path("/")
    public static class Oauth2TokenController {

        private final String clientId;
        private final String clientSecret;
        private final RSAKey rsaKey;

        public Oauth2TokenController(String clientId, String clientSecret) throws JOSEException {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.rsaKey = new RSAKeyGenerator(2048).keyID("key-1").generate();
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

        @GET
        @Path("/jwks")
        @Produces(APPLICATION_JSON)
        public Response jwks() {
            var jwkSet = new JWKSet(rsaKey.toPublicJWK());
            return Response.ok(jwkSet.toJSONObject()).build();
        }

        public String issueJwt(String sub) {
            var now = new Date();

            var claimsSet = new JWTClaimsSet.Builder()
                    .subject(sub)
                    .issuer("https://your-app.com")
                    .expirationTime(new Date(now.getTime() + 60_000))
                    .notBeforeTime(now)
                    .issueTime(now)
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            var header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(rsaKey.getKeyID())
                    .type(JOSEObjectType.JWT)
                    .build();

            var signedJwt = new SignedJWT(header, claimsSet);

            try {
                signedJwt.sign(new RSASSASigner(rsaKey));
            } catch (JOSEException e) {
                throw new RuntimeException(e);
            }

            return signedJwt.serialize();
        }

    }

}
