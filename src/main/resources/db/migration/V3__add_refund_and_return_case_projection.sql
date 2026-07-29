CREATE TABLE refund_payment (
    refund_id UUID PRIMARY KEY,
    return_id UUID NOT NULL,
    source_event_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    refund_minor_units BIGINT NOT NULL,
    refund_currency CHAR(3) NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    provider_reference VARCHAR(100),
    settled_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT refund_payment_return_unique UNIQUE (return_id),
    CONSTRAINT refund_payment_source_event_unique UNIQUE (source_event_id),
    CONSTRAINT refund_payment_provider_reference_unique UNIQUE (provider_reference),
    CONSTRAINT refund_payment_status CHECK (status IN ('SCHEDULED', 'COMPLETED')),
    CONSTRAINT refund_payment_amount_positive CHECK (refund_minor_units > 0),
    CONSTRAINT refund_payment_currency CHECK (refund_currency IN ('EUR', 'USD', 'GBP')),
    CONSTRAINT refund_payment_settlement_consistent CHECK (
        (status = 'SCHEDULED' AND provider_reference IS NULL AND settled_at IS NULL)
        OR
        (status = 'COMPLETED' AND provider_reference IS NOT NULL AND settled_at IS NOT NULL)
    )
);

CREATE INDEX refund_payment_status_scheduled_at_idx
    ON refund_payment (status, scheduled_at);

CREATE TABLE return_case_view (
    return_id UUID PRIMARY KEY,
    order_reference VARCHAR(64),
    item_reference VARCHAR(64),
    reason VARCHAR(32),
    workflow_stage VARCHAR(32) NOT NULL,
    workflow_rank SMALLINT NOT NULL,
    refund_minor_units BIGINT,
    refund_currency CHAR(3),
    inspection_outcome VARCHAR(16),
    rejection_reason VARCHAR(32),
    refund_id UUID,
    refund_status VARCHAR(16),
    last_updated_at TIMESTAMPTZ NOT NULL,
    projection_version BIGINT NOT NULL,
    CONSTRAINT return_case_view_stage CHECK (
        workflow_stage IN (
            'REQUESTED',
            'INSPECTED',
            'APPROVED',
            'REJECTED',
            'REFUND_SCHEDULED',
            'REFUNDED'
        )
    ),
    CONSTRAINT return_case_view_rank CHECK (workflow_rank IN (10, 20, 30, 40, 50)),
    CONSTRAINT return_case_view_stage_rank_consistent CHECK (
        (workflow_stage = 'REQUESTED' AND workflow_rank = 10)
        OR
        (workflow_stage = 'INSPECTED' AND workflow_rank = 20)
        OR
        (workflow_stage IN ('APPROVED', 'REJECTED') AND workflow_rank = 30)
        OR
        (workflow_stage = 'REFUND_SCHEDULED' AND workflow_rank = 40)
        OR
        (workflow_stage = 'REFUNDED' AND workflow_rank = 50)
    ),
    CONSTRAINT return_case_view_base_fields_consistent CHECK (
        (order_reference IS NULL AND item_reference IS NULL AND reason IS NULL)
        OR
        (order_reference IS NOT NULL AND item_reference IS NOT NULL AND reason IS NOT NULL)
    ),
    CONSTRAINT return_case_view_amount_positive CHECK (
        refund_minor_units IS NULL OR refund_minor_units > 0
    ),
    CONSTRAINT return_case_view_currency CHECK (
        refund_currency IS NULL OR refund_currency IN ('EUR', 'USD', 'GBP')
    ),
    CONSTRAINT return_case_view_inspection_outcome CHECK (
        inspection_outcome IS NULL OR inspection_outcome IN ('ACCEPTED', 'REJECTED')
    ),
    CONSTRAINT return_case_view_rejection_reason CHECK (
        rejection_reason IS NULL OR rejection_reason = 'INSPECTION_FAILED'
    ),
    CONSTRAINT return_case_view_refund_fields_consistent CHECK (
        (refund_id IS NULL AND refund_status IS NULL)
        OR
        (refund_id IS NOT NULL AND refund_status IN ('SCHEDULED', 'COMPLETED'))
    ),
    CONSTRAINT return_case_view_projection_version_positive CHECK (projection_version > 0)
);

CREATE INDEX return_case_view_stage_updated_at_idx
    ON return_case_view (workflow_stage, last_updated_at DESC);

CREATE INDEX return_case_view_order_reference_idx
    ON return_case_view (order_reference)
    WHERE order_reference IS NOT NULL;

CREATE TABLE return_case_projection_event (
    event_id UUID PRIMARY KEY,
    return_id UUID NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    projected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX return_case_projection_event_return_id_idx
    ON return_case_projection_event (return_id, occurred_at);
