-- JPA maps currency codes as bounded strings. Keep the database representation
-- aligned so Hibernate schema validation exercises the same contract in every
-- runtime environment.
ALTER TABLE return_request
    ALTER COLUMN refund_currency TYPE VARCHAR(3);

ALTER TABLE inspection_case
    ALTER COLUMN refund_currency TYPE VARCHAR(3);

ALTER TABLE return_resolution
    ALTER COLUMN refund_currency TYPE VARCHAR(3);

ALTER TABLE refund_payment
    ALTER COLUMN refund_currency TYPE VARCHAR(3);

ALTER TABLE return_case_view
    ALTER COLUMN refund_currency TYPE VARCHAR(3);
