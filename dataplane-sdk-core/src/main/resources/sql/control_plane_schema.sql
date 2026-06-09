--
--  Copyright (c) 2026 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial script
--

-- THIS SCHEMA HAS BEEN WRITTEN AND TESTED ONLY FOR POSTGRES

CREATE TABLE IF NOT EXISTS control_planes
(
    id              VARCHAR     PRIMARY KEY,
    endpoint        VARCHAR,
    auth            JSON
);

COMMENT ON COLUMN control_planes.auth IS 'Authorization profile serialized as JSON';
