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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import org.eclipse.dataplane.port.exception.IllegalAttributeTypeException;

import java.util.HashMap;
import java.util.Map;

public class AuthorizationProfile {

    private final Map<String, Object> attributes;

    public AuthorizationProfile() {
        attributes = new HashMap<>();
    }

    public AuthorizationProfile(String type) {
        this();
        attributes.put("type", type);
    }

    public String getType() {
        return attributes.get("type").toString();
    }

    @JsonAnyGetter
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public String stringAttribute(String key) {
        var attribute = attributes.get(key);
        if (attribute == null) {
            return null;
        }

        if (attribute instanceof String stringAttribute) {
            return stringAttribute;
        }

        throw new IllegalAttributeTypeException("Attribute %s is not a String but it's a %s".formatted(key, attribute.getClass().getSimpleName()));
    }

    public AuthorizationProfile withAttribute(String key, String value) {
        setAttribute(key, value);
        return this;
    }
}
