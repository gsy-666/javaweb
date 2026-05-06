ALTER TABLE work_order
  ADD COLUMN deleted_at DATETIME NULL AFTER cancelled_at;
