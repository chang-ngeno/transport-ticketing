# Transport Ticketing System

**Version**: 4.0.0  
**Stack**: Java 21 + Spring Boot 3.3.4 + TimescaleDB (PostgreSQL)

## Features
- Multi-tenant architecture with per-tenant encrypted M-PESA credentials
- Dynamic pricing by `LocalDateTime` (fare windows with `effectiveFrom` / `effectiveTo`)
- Virtual Threads + Structured Concurrency for parallel STK push + SMS
- Roles: `SUPER_ADMIN`, `TENANT_ADMIN`, `STAGE_HEAD`, `STAGE_ATTENDANT`
- JWT authentication, Resilience4j rate limiting
- Africa's Talking SMS integration
- TimescaleDB hypertable on `booking.created_at`
- Flyway migrations (V1–V5)

## Project Structure
```
src/main/java/ke/co/masajr/transport/
├── TransportTicketingApplication.java
├── entity/       Role, Tenant, AppUser, Stage, Trip, Vehicle, Fare, BookingEntity
├── repository/   AppUserRepository, TenantRepository, StageRepository,
│                 TripRepository, FareRepository, VehicleRepository, BookingRepository
├── service/      EncryptionService, SmsService, MpesaService,
│                 TenantService, TicketBookingService
├── config/       JwtUtil, JwtFilter, SecurityConfig
└── controller/   AuthController, TenantController, TicketController, MpesaCallbackController

src/main/resources/
├── application.yml
└── db/migration/
    ├── V1__initial_schema.sql
    ├── V2__add_per_tenant_salt.sql
    ├── V3__add_stage_and_update_trip.sql
    ├── V4__add_fare_and_vehicle.sql
    └── V5__fare_timestamp_precision.sql
```

## Prerequisites
- Java 21
- PostgreSQL with TimescaleDB extension
- Maven 3.9+

## Setup
1. Copy `.env.example` to `.env` and fill in values.
2. Create the database:
   ```sql
   CREATE DATABASE ticketdb;
   \c ticketdb
   CREATE EXTENSION IF NOT EXISTS timescaledb;
   ```
3. Run:
   ```bash
   export $(cat .env | xargs)
   mvn clean spring-boot:run -Dspring-boot.run.jvmArguments="--enable-preview"
   ```

## API Quick Reference

### Auth
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/login` | None | Get JWT token |

### Admin (SUPER_ADMIN)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/admin/tenants` | Create tenant |
| GET  | `/api/admin/tenants` | List tenants |

### Tenant Admin
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/tenant/users` | Create user |
| POST | `/api/tenant/stages` | Create stage |
| GET  | `/api/tenant/stages` | List stages |
| POST | `/api/tenant/trips` | Create trip |
| GET  | `/api/tenant/trips` | List trips |
| POST | `/api/tenant/trips/{tripId}/fares` | Set dynamic fare |
| GET  | `/api/tenant/trips/{tripId}/fares` | List fares |

### Stage Management
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/stage/vehicles` | Add vehicle |
| GET  | `/api/stage/vehicles` | List vehicles |
| PATCH | `/api/stage/vehicles/{id}/toggle` | Activate/deactivate |

### Tickets
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/tickets/book` | Book single ticket |
| POST | `/api/tickets/book/batch` | Book multiple tickets |
| GET  | `/api/tickets/{ticketId}` | Get ticket |
| GET  | `/api/tickets` | List all bookings (tenant) |

### M-PESA Callback
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/mpesa/callback` | Safaricom STK callback (public) |

## Dynamic Pricing Logic
Pricing resolves the active `Fare` whose window covers the trip's `departureTime`.  
Falls back to the trip's `basePrice` if no fare window matches.

```
POST /api/tenant/trips/{tripId}/fares
{
  "effectiveFrom": "2026-04-01T06:00:00",
  "effectiveTo":   "2026-04-01T09:00:00",
  "price": 150.00
}
```

## Roles & Permissions
| Action | SUPER_ADMIN | TENANT_ADMIN | STAGE_HEAD | STAGE_ATTENDANT |
|--------|:-----------:|:------------:|:----------:|:---------------:|
| Manage tenants | ✅ | ❌ | ❌ | ❌ |
| Manage users | ✅ | ✅ | ❌ | ❌ |
| Manage stages/trips | ✅ | ✅ | ❌ | ❌ |
| Manage fares | ✅ | ✅ | ✅ | ❌ |
| Manage vehicles | ✅ | ✅ | ✅ | ❌ |
| Book tickets | ✅ | ✅ | ✅ | ✅ |
