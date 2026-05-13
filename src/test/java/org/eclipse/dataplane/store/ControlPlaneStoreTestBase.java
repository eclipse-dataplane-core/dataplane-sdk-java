/*
 *  Copyright (c) 2026 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.dataplane.store;

import org.eclipse.dataplane.domain.controlplane.ControlPlane;
import org.eclipse.dataplane.domain.registration.AuthorizationProfile;
import org.eclipse.dataplane.port.exception.ResourceNotFoundException;
import org.eclipse.dataplane.port.store.ControlPlaneStore;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class ControlPlaneStoreTestBase {

    @Nested
    class Save {
        @Test
        void save_newControlPlane_shouldCreate() {
            var id = "id";
            var controlPlane = controlPlane(id);

            var result = store().save(controlPlane);
            assertThat(result.succeeded()).isTrue();

            var persisted = store().findById(id).getContent();
            assertThat(persisted).isNotNull();
            assertThat(persisted).usingRecursiveComparison().isEqualTo(controlPlane);
        }

        @Test
        void save_existingControlPlane_shouldUpdate() {
            var id = "toUpdate";
            var controlPlane = controlPlane(id);
            store().save(controlPlane);

            var newControlPlane = ControlPlane.newInstance()
                    .id(id)
                    .endpoint(URI.create("http://some-other-endpoint"))
                    .authorization(controlPlane.getAuthorization())
                    .build();
            store().save(newControlPlane);

            var updated = store().findById(id).getContent();
            assertThat(updated.getEndpoint())
                    .isEqualTo(newControlPlane.getEndpoint())
                    .isNotEqualTo(controlPlane.getEndpoint());
            assertThat(updated.getAuthorization()).usingRecursiveComparison()
                    .isEqualTo(controlPlane.getAuthorization())
                    .isEqualTo(newControlPlane.getAuthorization());
        }
    }

    @Nested
    class FindById {
        @Test
        void findById_exists_shouldReturnControlPlane() {
            var id = "id";
            var controlPlane = controlPlane(id);
            store().save(controlPlane);

            var result = store().findById(id);

            assertThat(result.succeeded()).isTrue();
            assertThat(result.getContent()).isNotNull();
        }

        @Test
        void findById_doesNotExist_shouldReturnNotFound() {
            var result = store().findById("nonExistent");

            assertThat(result.failed()).isTrue();
            assertThat(result.getException()).isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class Delete {
        @Test
        void delete_exists_shouldDelete() {
            var id = "id";
            var controlPlane = controlPlane(id);
            store().save(controlPlane);

            var deleteResult = store().delete(id);
            assertThat(deleteResult.succeeded()).isTrue();

            var findResult = store().findById(id);
            assertThat(findResult.failed()).isTrue();
            assertThat(findResult.getException()).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void delete_doesNotExist_shouldReturnNotFound() {
             var deleteResult = store().delete("nonExistent");

            assertThat(deleteResult.failed()).isTrue();
            assertThat(deleteResult.getException()).isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class Exists {
        @Test
        void exists_exists_shouldReturnTrue() {
            var id = "id";
            var controlPlane = controlPlane(id);
            store().save(controlPlane);

            var exists = store().exists(id);

            assertThat(exists).isTrue();
        }

        @Test
        void exists_doesNotExists_shouldReturnFalse() {
            var exists = store().exists("nonExistent");

            assertThat(exists).isFalse();
        }
    }

    protected abstract ControlPlaneStore store();

    private ControlPlane controlPlane(String id) {
        return ControlPlane.newInstance()
                .id(id)
                .endpoint(URI.create("https://controlplane"))
                .authorization(new AuthorizationProfile("token")
                        .withAttribute("Authorization", "authToken"))
                .build();
    }
}
