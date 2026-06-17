ALTER TABLE vehicle ADD COLUMN tenant_id BIGINT;
ALTER TABLE trip ADD COLUMN vehicle_id BIGINT;
ALTER TABLE trip ADD COLUMN to_stage_id BIGINT;
ALTER TABLE booking ADD COLUMN payment_method VARCHAR(10) DEFAULT 'CASH';

-- Backfill tenant_id for existing vehicles
UPDATE vehicle v SET tenant_id = (SELECT tenant_id FROM stage s WHERE s.id = v.stage_id);

-- Backfill vehicle_id for existing trips.
-- Prefer a vehicle at the trip's from_stage_id, then any vehicle in the same tenant.
UPDATE trip t SET vehicle_id = COALESCE(
    (SELECT v.id FROM vehicle v WHERE v.stage_id = t.from_stage_id ORDER BY v.id LIMIT 1),
    (SELECT v.id FROM vehicle v JOIN stage s ON s.id = v.stage_id WHERE s.tenant_id = t.tenant_id ORDER BY v.id LIMIT 1),
    (SELECT id FROM vehicle ORDER BY id LIMIT 1)
);

ALTER TABLE vehicle ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE trip ALTER COLUMN vehicle_id SET NOT NULL;