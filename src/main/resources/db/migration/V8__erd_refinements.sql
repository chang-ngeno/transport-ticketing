-- Rename departure_time to trip_start_time in trip table
ALTER TABLE trip RENAME COLUMN departure_time TO trip_start_time;

-- Add missing foreign key constraints
ALTER TABLE vehicle ADD CONSTRAINT fk_vehicle_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE;
ALTER TABLE trip ADD CONSTRAINT fk_trip_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(id) ON DELETE CASCADE;
ALTER TABLE trip ADD CONSTRAINT fk_trip_to_stage FOREIGN KEY (to_stage_id) REFERENCES stage(id) ON DELETE SET NULL;
ALTER TABLE app_user ADD CONSTRAINT fk_app_user_stage FOREIGN KEY (stage_id) REFERENCES stage(id) ON DELETE SET NULL;

-- Add status column to vehicle table
ALTER TABLE vehicle ADD COLUMN status VARCHAR(20) DEFAULT 'available' CHECK (status IN ('available', 'boarding', 'travelling'));

-- Add status column to trip table
ALTER TABLE trip ADD COLUMN status VARCHAR(20) DEFAULT 'BOARDING' CHECK (status IN ('BOARDING', 'TRAVELLING', 'ENDED'));

-- Add passenger_count column to booking table (default 0)
ALTER TABLE booking ADD COLUMN passenger_count INT DEFAULT 0;
