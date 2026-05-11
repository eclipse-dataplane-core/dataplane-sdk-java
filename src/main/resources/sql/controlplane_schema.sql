CREATE TABLE IF NOT EXISTS control_planes
(
    id              VARCHAR     PRIMARY KEY,
    endpoint        VARCHAR,
    authorization   JSON
);

COMMENT ON COLUMN control_planes.authorization IS 'Authorization profile serialized as JSON';
