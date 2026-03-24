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

package org.eclipse.dataplane.domain.controlplane;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataplane.domain.registration.AuthorizationProfile;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ControlPlane {

    private String id;
    private URI endpoint;
    private final List<AuthorizationProfile> authorizations = new ArrayList<>();

    public String getId() {
        return id;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public static ControlPlane.Builder newInstance() {
        return new ControlPlane.Builder();
    }

    public List<AuthorizationProfile> getAuthorizations() {
        return authorizations;
    }

    public AuthorizationProfile authorization() {
        return getAuthorizations().stream().findAny().orElse(null);
    }

    @JsonPOJOBuilder
    public static class Builder {
        private final ControlPlane controlPlane = new ControlPlane();

        private Builder() {

        }

        public ControlPlane build() {
            Objects.requireNonNull(controlPlane.id);

            return controlPlane;
        }

        public Builder id(String id) {
            controlPlane.id = id;
            return this;
        }

        public Builder endpoint(URI endpoint) {
            controlPlane.endpoint = endpoint;
            return this;
        }

        public Builder authorization(List<AuthorizationProfile> authorizations) {
            controlPlane.authorizations.addAll(authorizations);
            return this;
        }
    }
}
