ALTER TABLE projects ADD COLUMN monthly_fee_rub NUMERIC;
ALTER TABLE projects ADD COLUMN monthly_fee_eur NUMERIC;

UPDATE projects SET monthly_fee_rub = episode_cost_rub, monthly_fee_eur = 1;
