-- Allow the same ten supported currencies used by the native FinCip app.
-- Safe to re-run after the original schema.sql.
alter table public.shared_budgets
  drop constraint if exists shared_budgets_currency_check;

alter table public.shared_budgets
  add constraint shared_budgets_currency_check
  check (currency in ('INR', 'USD', 'EUR', 'GBP', 'JPY', 'CNY', 'CHF', 'CAD', 'AUD', 'SGD'));
