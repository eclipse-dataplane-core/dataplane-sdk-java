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

import org.eclipse.dataplane.Dataplane;
import org.eclipse.dataplane.domain.DataAddress;
import org.eclipse.dataplane.domain.Result;
import org.eclipse.dataplane.domain.dataflow.DataFlow;
import org.eclipse.dataplane.domain.registration.Authorization;
import org.eclipse.dataplane.domain.registration.AuthorizationProfile;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;

public class TckDataplane {

    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();

    private final Dataplane dataplane;

    public TckDataplane(String tckControlPlaneId) {
        this.dataplane = Dataplane.newInstance()
                .id("sdk-dataplane")
                .registerAuthorization(new TckAuthorization(tckControlPlaneId))
                .onPrepare(this::handlePrepare)
                .onStart(this::handleStart)
                .onStarted(this::handleStarted)
                .onSuspend(Result::success)
                .onResume(Result::success)
                .onTerminate(Result::success)
                .onCompleted(Result::success)
                .build();
    }

    public Dataplane getDataplane() {
        return dataplane;
    }

    private Result<DataFlow> handlePrepare(DataFlow dataFlow) {
        if ("async".equals(dataFlow.getAgreementId())) {
            dataFlow.transitionToPreparing();
            var dataFlowId = dataFlow.getId();
            EXECUTOR_SERVICE.schedule(() -> {
                var result = dataplane.notifyPrepared(dataFlowId, Result::success);
                if (result.failed()) {
                    throw new RuntimeException("[TCK async-prepare] notifyPrepared failed: " + result.getException());
                }
            }, 50,  TimeUnit.MILLISECONDS);
        } else {
            dataFlow.setDataAddress(new DataAddress("endpointType", "http://any", emptyList()));
        }
        return Result.success(dataFlow);
    }

    private Result<DataFlow> handleStart(DataFlow dataFlow) {
        var dataFlowId = dataFlow.getId();
        if ("async".equals(dataFlow.getAgreementId())) {
            dataFlow.transitionToStarting();
            EXECUTOR_SERVICE.schedule(() -> {
                var result = dataplane.notifyStarted(dataFlowId, Result::success);
                if (result.failed()) {
                    throw new RuntimeException("[TCK async-start] notifyStarted failed: " + result.getException());
                }
            }, 50, TimeUnit.MILLISECONDS);
        } else {
            dataFlow.setDataAddress(new DataAddress("endpointType", "http://any", emptyList()));
            if ("complete".equals(dataFlow.getAgreementId())) {
                EXECUTOR_SERVICE.schedule(() -> {
                    var result = dataplane.notifyCompleted(dataFlowId);
                    if (result.failed()) {
                        throw new RuntimeException("[TCK auto-complete] notifyCompleted failed: " + result.getException());
                    }
                }, 250, TimeUnit.MILLISECONDS);
            }
        }
        return Result.success(dataFlow);
    }

    private Result<DataFlow> handleStarted(DataFlow dataFlow) {
        if ("complete".equals(dataFlow.getAgreementId())) {
            var dataFlowId = dataFlow.getId();
            EXECUTOR_SERVICE.schedule(() -> {
                var result = dataplane.notifyCompleted(dataFlowId);
                if (result.failed()) {
                    throw new RuntimeException("[TCK auto-complete] notifyCompleted failed: " + result.getException());
                }
            }, 250, TimeUnit.MILLISECONDS);
        }
        return Result.success(dataFlow);
    }

    static class TckAuthorization implements Authorization {

        private final String tckControlPlaneId;

        TckAuthorization(String tckControlPlaneId) {
            this.tckControlPlaneId = tckControlPlaneId;
        }

        @Override
        public String type() {
            return "tck";
        }

        @Override
        public Result<String> authorizationHeader(AuthorizationProfile profile) {
            return Result.success("dummy");
        }

        @Override
        public Result<String> extractCallerId(String authorizationHeader) {
            return Result.success(tckControlPlaneId);
        }
    }

}
