CREATE TABLE payments_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    participant_id INTEGER NOT NULL,
    project_id INTEGER NOT NULL,
    payment_date TEXT NOT NULL,
    source TEXT NOT NULL CHECK (source IN ('VK_DONUT', 'MANUAL')),
    amount_original NUMERIC NOT NULL CHECK (amount_original > 0),
    currency TEXT NOT NULL CHECK (currency IN ('RUB', 'USD', 'EUR')),
    exchange_rate NUMERIC NOT NULL CHECK (exchange_rate > 0),
    amount_rub NUMERIC NOT NULL CHECK (amount_rub > 0),
    fee_percent INTEGER NOT NULL CHECK (fee_percent >= 0 AND fee_percent <= 100),
    net_amount_rub NUMERIC NOT NULL CHECK (net_amount_rub >= 0),
    comment TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'VOIDED')),
    FOREIGN KEY (participant_id) REFERENCES participants(id),
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

INSERT INTO payments_new (
    id, participant_id, project_id, payment_date, source,
    amount_original, currency, exchange_rate, amount_rub,
    fee_percent, net_amount_rub, comment, status
)
SELECT
    id, participant_id, project_id, payment_date,
    CASE WHEN source = 'OTHER' THEN 'MANUAL' ELSE source END,
    amount_original, currency, exchange_rate, amount_rub,
    fee_percent, net_amount_rub, comment, status
FROM payments;

DROP TABLE payments;

ALTER TABLE payments_new RENAME TO payments;

CREATE INDEX idx_payments_participant_id ON payments(participant_id);
CREATE INDEX idx_payments_project_id ON payments(project_id);
CREATE INDEX idx_payments_payment_date ON payments(payment_date);
