ALTER TABLE stock_sale_history
    ADD COLUMN IF NOT EXISTS deduction_amount_krw NUMERIC(18, 2),
    ADD COLUMN IF NOT EXISTS net_proceeds_krw NUMERIC(18, 2),
    ADD COLUMN IF NOT EXISTS net_profit_krw NUMERIC(18, 2),
    ADD COLUMN IF NOT EXISTS net_profit_rate NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS net_contribution_rate NUMERIC(10, 2);

UPDATE stock_sale_history
SET deduction_amount_krw = COALESCE(deduction_amount_krw, 0),
    net_proceeds_krw = COALESCE(net_proceeds_krw, sale_price_krw),
    net_profit_krw = COALESCE(net_profit_krw, profit_krw),
    net_profit_rate = COALESCE(net_profit_rate, profit_rate),
    net_contribution_rate = COALESCE(net_contribution_rate, contribution_rate)
WHERE deduction_amount_krw IS NULL
   OR net_proceeds_krw IS NULL
   OR net_profit_krw IS NULL
   OR net_profit_rate IS NULL
   OR net_contribution_rate IS NULL;
