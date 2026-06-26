ALTER TABLE project_memberships ADD COLUMN billing_mode TEXT NOT NULL DEFAULT 'SUBSCRIPTION'
    CHECK (billing_mode IN ('SUBSCRIPTION', 'PACKAGE'));
ALTER TABLE project_memberships ADD COLUMN paid_until_month TEXT;
ALTER TABLE project_memberships ADD COLUMN partial_payment_amount NUMERIC;
ALTER TABLE project_memberships ADD COLUMN partial_payment_currency TEXT
    CHECK (partial_payment_currency IS NULL OR partial_payment_currency IN ('RUB', 'USD', 'EUR'));
