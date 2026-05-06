ALTER TABLE work_order
  ADD COLUMN eta_at DATETIME NULL AFTER accepted_at;
