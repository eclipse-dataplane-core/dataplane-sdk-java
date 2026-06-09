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

package org.eclipse.dataplane.tck;

import org.eclipse.dataplane.HttpServer;
import org.eclipse.dataplane.domain.registration.ControlPlaneRegistrationMessage;
import org.eclipse.dataplane.port.DataPlaneSignalingApiController;
import org.eclipse.dataspacetck.dps.system.DpsSystemLauncher;
import org.eclipse.dataspacetck.runtime.TckRuntime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the DPS TCK data plane verification tests against the dataplane-sdk-java HTTP server.
 *
 * <p>The SDK data plane (the CUT) is started in {@code @BeforeAll}. The TCK uses DpsSystemLauncher as the simulated
 * control plane, pointed at the SDK server
 * via the {@code dataspacetck.dps.dataplane.url} property.
 *
 * <p>Tests signal their intent via a sentinel {@code agreementId} in {@code dps.tck.properties}:
 * {@code "complete"} causes the data plane to autonomously send a completed callback after the
 * transfer starts; {@code "async"} causes it to respond 202+transitional then send a started/prepared callback.
 */
public class DpsTckTest {

    private HttpServer httpServer;

    @BeforeEach
    void startDataplane() {
        var tckControlPlaneId = "tck-control-plane";
        var tckDataplane = new TckDataplane(tckControlPlaneId);
        tckDataplane.getDataplane().registerControlPlane(new ControlPlaneRegistrationMessage(tckControlPlaneId, URI.create("http://localhost")));

        httpServer = new HttpServer();
        httpServer.start();
        httpServer.deploy("/dataplane", new DataPlaneSignalingApiController(tckDataplane.getDataplane()));
    }

    @AfterEach
    void stopDataplane() {
        if (httpServer != null) {
            httpServer.stop();
        }
    }

    @Test
    void runDpsTckTests() {
        var properties = loadProperties("dps.tck.properties");
        properties.put("dataspacetck.debug", "true");
        properties.put("dataspacetck.dps.dataplane.url", "http://localhost:" + httpServer.port() + "/dataplane/v1");

        var result = TckRuntime.Builder.newInstance()
                .launcher(DpsSystemLauncher.class)
                .properties(properties)
                .addPackage("org.eclipse.dataspacetck.dps.verification.dataplane")
                .build()
                .execute();

        assertThat(result.getFailures()).isEmpty();
    }

    private Map<String, String> loadProperties(String resource) {
        var url = getClass().getClassLoader().getResource(resource);
        assertThat(url).as("Resource not found: %s", resource).isNotNull();
        var properties = new Properties();
        try (var stream = url.openStream()) {
            properties.load(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
    }

}
