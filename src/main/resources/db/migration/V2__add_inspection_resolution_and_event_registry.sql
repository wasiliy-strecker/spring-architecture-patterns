CREATE TABLE inspection_case (
    return_id UUID PRIMARY KEY,
    source_event_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    refund_minor_units BIGINT NOT NULL,
    refund_currency CHAR(3) NOT NULL,
    outcome VARCHAR(16),
    note VARCHAR(500),
    registered_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT inspection_case_source_event_unique UNIQUE (source_event_id),
    CONSTRAINT inspection_case_status CHECK (status IN ('PENDING', 'COMPLETED')),
    CONSTRAINT inspection_case_outcome CHECK (outcome IN ('ACCEPTED', 'REJECTED')),
    CONSTRAINT inspection_case_refund_positive CHECK (refund_minor_units > 0),
    CONSTRAINT inspection_case_currency CHECK (refund_currency IN ('EUR', 'USD', 'GBP')),
    CONSTRAINT inspection_case_completion_consistent CHECK (
        (status = 'PENDING' AND outcome IS NULL AND completed_at IS NULL)
        OR
        (status = 'COMPLETED' AND outcome IS NOT NULL AND completed_at IS NOT NULL)
    )
);

CREATE INDEX inspection_case_status_registered_at_idx
    ON inspection_case (status, registered_at);

CREATE TABLE return_resolution (
    return_id UUID PRIMARY KEY,
    source_event_id UUID NOT NULL,
    decision_event_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    refund_minor_units BIGINT NOT NULL,
    refund_currency CHAR(3) NOT NULL,
    rejection_reason VARCHAR(32),
    decided_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT return_resolution_source_event_unique UNIQUE (source_event_id),
    CONSTRAINT return_resolution_decision_event_unique UNIQUE (decision_event_id),
    CONSTRAINT return_resolution_status CHECK (status IN ('APPROVED', 'REJECTED')),
    CONSTRAINT return_resolution_refund_positive CHECK (refund_minor_units > 0),
    CONSTRAINT return_resolution_currency CHECK (refund_currency IN ('EUR', 'USD', 'GBP')),
    CONSTRAINT return_resolution_decision_consistent CHECK (
        (status = 'APPROVED' AND rejection_reason IS NULL)
        OR
        (status = 'REJECTED' AND rejection_reason = 'INSPECTION_FAILED')
    )
);

CREATE INDEX return_resolution_status_decided_at_idx
    ON return_resolution (status, decided_at DESC);

-- Spring Modulith JDBC Event Publication Registry, managed here so Flyway remains
-- the single schema authority in every environment.
CREATE TABLE event_publication (
    id UUID NOT NULL,
    listener_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMPTZ NOT NULL,
    completion_date TIMESTAMPTZ,
    status TEXT,
    completion_attempts INT,
    last_resubmission_date TIMESTAMPTZ,
    PRIMARY KEY (id)
);

CREATE INDEX event_publication_serialized_event_hash_idx
    ON event_publication USING hash (serialized_event);

CREATE INDEX event_publication_completion_date_idx
    ON event_publication (completion_date);
