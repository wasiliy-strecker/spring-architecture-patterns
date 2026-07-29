CREATE TABLE return_request (
    id UUID PRIMARY KEY,
    order_reference VARCHAR(64) NOT NULL,
    item_reference VARCHAR(64) NOT NULL,
    reason VARCHAR(32) NOT NULL,
    comment VARCHAR(500),
    refund_minor_units BIGINT NOT NULL,
    refund_currency CHAR(3) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT return_request_order_reference_length
        CHECK (char_length(order_reference) BETWEEN 3 AND 64),
    CONSTRAINT return_request_item_reference_length
        CHECK (char_length(item_reference) BETWEEN 3 AND 64),
    CONSTRAINT return_request_reason
        CHECK (reason IN (
            'DAMAGED',
            'NOT_AS_DESCRIBED',
            'WRONG_ITEM',
            'NO_LONGER_NEEDED'
        )),
    CONSTRAINT return_request_refund_positive
        CHECK (refund_minor_units > 0),
    CONSTRAINT return_request_currency
        CHECK (refund_currency IN ('EUR', 'USD', 'GBP')),
    CONSTRAINT return_request_order_item_unique
        UNIQUE (order_reference, item_reference)
);

CREATE INDEX return_request_requested_at_idx
    ON return_request (requested_at DESC);
