CREATE TABLE payments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    participant_id INTEGER NOT NULL,
    project_id INTEGER NOT NULL,
    payment_date TEXT NOT NULL,
    source TEXT NOT NULL CHECK (source IN ('VK_DONUT', 'OTHER')),
    amount_original NUMERIC NOT NULL CHECK (amount_original > 0),
    currency TEXT NOT NULL CHECK (currency IN ('RUB', 'USD', 'EUR')),
    exchange_rate NUMERIC NOT NULL CHECK (exchange_rate > 0),
    amount_rub NUMERIC NOT NULL CHECK (amount_rub > 0),
    fee_percent INTEGER NOT NULL CHECK (fee_percent >= 0 AND fee_percent <= 100),
    net_amount_rub NUMERIC NOT NULL CHECK (net_amount_rub >= 0),
    comment TEXT,
    FOREIGN KEY (participant_id) REFERENCES participants(id),
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE INDEX idx_payments_participant_id ON payments(participant_id);
CREATE INDEX idx_payments_project_id ON payments(project_id);
CREATE INDEX idx_payments_payment_date ON payments(payment_date);
