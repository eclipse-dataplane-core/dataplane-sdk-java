CREATE TABLE IF NOT EXISTS data_flows
(
    id                      VARCHAR     PRIMARY KEY,
    transfer_type           VARCHAR,
    type                    VARCHAR,
    state                   VARCHAR     NOT NULL,
    dataset_id              VARCHAR,
    agreement_id            VARCHAR,
    participant_id          VARCHAR,
    counter_party_id        VARCHAR,
    dataspace_context       VARCHAR,
    callback_address        VARCHAR,
    suspension_reason       VARCHAR,
    termination_reason      VARCHAR,
    labels                  JSON,
    metadata                JSON,
    data_address            JSON,
    controlplane_id         VARCHAR
);

COMMENT ON COLUMN data_flows.labels IS 'List of labels serialized as JSON';
COMMENT ON COLUMN data_flows.metadata IS 'Metadata serialized as JSON';
COMMENT ON COLUMN data_flows.data_address IS 'Data address serialized as JSON';
