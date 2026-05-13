CREATE TABLE IF NOT EXISTS control_planes
(
    id              VARCHAR     PRIMARY KEY,
    endpoint        VARCHAR,
    auth            JSON
);

COMMENT ON COLUMN control_planes.auth IS 'Authorization profile serialized as JSON';
