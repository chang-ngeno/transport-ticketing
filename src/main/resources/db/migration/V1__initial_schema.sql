CREATE TABLE tenant (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    mpesa_shortcode VARCHAR(20) NOT NULL,
    mpesa_consumer_key_encrypted TEXT,
    mpesa_consumer_secret_encrypted TEXT,
    mpesa_passkey_encrypted TEXT,
    mpesa_encryption_salt VARCHAR(16) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('SUPER_ADMIN', 'TENANT_ADMIN', 'STAGE_HEAD', 'STAGE_ATTENDANT')),
    tenant_id BIGINT REFERENCES tenant(id),
    stage_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE stage (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    name VARCHAR(100) NOT NULL,
    location VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE trip (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    from_stage_id BIGINT REFERENCES stage(id),
    to_destination VARCHAR(200),
    route VARCHAR(200),
    departure_time TIMESTAMP NOT NULL,
    total_seats INT NOT NULL,
    booked_seats INT DEFAULT 0,
    price_per_seat DECIMAL(10,2) NOT NULL
);

CREATE TABLE booking (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    trip_id BIGINT NOT NULL REFERENCES trip(id),
    ticket_id VARCHAR(50) UNIQUE NOT NULL,
    checkout_request_id VARCHAR(100) UNIQUE,
    phone_number VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    price_paid DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID', 'FAILED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE fare (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trip(id) ON DELETE CASCADE,
    effective_from TIMESTAMP NOT NULL,
    effective_to TIMESTAMP,
    price_per_seat DECIMAL(10,2) NOT NULL,
    created_by BIGINT REFERENCES app_user(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vehicle (
    id BIGSERIAL PRIMARY KEY,
    stage_id BIGINT NOT NULL REFERENCES stage(id) ON DELETE CASCADE,
    registration_number VARCHAR(20) UNIQUE NOT NULL,
    capacity INT NOT NULL CHECK (capacity > 0),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Conditionally convert booking to a TimescaleDB hypertable only when the
-- TimescaleDB extension is already installed on the database. This avoids
-- startup failures on plain PostgreSQL where the function does not exist.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'timescaledb') THEN
        PERFORM create_hypertable('booking', 'created_at', if_not_exists => TRUE);
    END IF;
END$$;

CREATE INDEX idx_booking_checkout ON booking(checkout_request_id);
CREATE INDEX idx_booking_tenant ON booking(tenant_id);
CREATE INDEX idx_fare_trip_time ON fare(trip_id, effective_from);
CREATE INDEX idx_vehicle_stage ON vehicle(stage_id);
