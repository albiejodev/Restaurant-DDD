-- Replication progress belongs to SymmetricDS node infrastructure, not this single-database demo.
ALTER TABLE branch_master_assignment DROP COLUMN synced_version;
ALTER TABLE branch_master_assignment DROP COLUMN last_synced_at;
