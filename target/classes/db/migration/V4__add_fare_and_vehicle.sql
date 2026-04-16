CREATE TABLE IF NOT EXISTS fare (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trip(id) ON DELETE CASCADE,
    effective_from TIMESTAMP NOT NULL,
    effective_to TIMESTAMP,
    price_per_seat DECIMAL(10,2) NOT NULL,
    created_by BIGINT REFERENCES app_user(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vehicle (
    id BIGSERIAL PRIMARY KEY,
    stage_id BIGINT NOT NULL REFERENCES stage(id) ON DELETE CASCADE,
    registration_number VARCHAR(20) UNIQUE NOT NULL,
    capacity INT NOT NULL CHECK (capacity > 0),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fare_trip_time ON fare(trip_id, effective_from);
CREATE INDEX IF NOT EXISTS idx_vehicle_stage ON vehicle(stage_id);
