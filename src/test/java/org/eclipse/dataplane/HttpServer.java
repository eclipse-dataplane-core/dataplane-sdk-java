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

package org.eclipse.dataplane;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.servlet.Source;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import static org.eclipse.jetty.ee10.servlet.ServletContextHandler.NO_SESSIONS;

public class HttpServer {

    private final Server server;
    private final ServletContextHandler servletContextHandler = new ServletContextHandler(NO_SESSIONS);
    private final int port;

    public HttpServer(int port) {
        this.port = port;
        server = new Server();
        var connector = new ServerConnector(server);
        connector.setPort(port);
        server.setConnectors(new Connector[]{connector});
        server.setHandler(servletContextHandler);
    }

    public void start() {
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deploy(String basePath, Object controller) {
        var servletHolder = createServletHolder(controller);
        servletContextHandler.getServletHandler().addServletWithMapping(servletHolder, basePath + "/*");
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ServletHolder createServletHolder(Object controller) {
        var resourceConfig = new ResourceConfig();
        resourceConfig.registerClasses(controller.getClass());
        resourceConfig.registerInstances(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(controller).to((Class<? super Object>) controller.getClass());
            }
        });
        var servlet = new ServletContainer(resourceConfig);
        var servletHolder = new ServletHolder(Source.EMBEDDED);
        servletHolder.setServlet(servlet);
        return servletHolder;
    }

    public int port() {
        return port;
    }
}
