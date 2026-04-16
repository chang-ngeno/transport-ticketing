package ke.co.masajr.transport.controller;

import jakarta.validation.Valid;
import ke.co.masajr.transport.dto.*;
import ke.co.masajr.transport.entity.*;
import ke.co.masajr.transport.service.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    // ── Tenants (SUPER_ADMIN only) ────────────────────────────────────────────

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/admin/tenants")
    public ResponseEntity<Tenant> createTenant(@Valid @RequestBody CreateTenantRequest req) {
        return ResponseEntity.ok(tenantService.createTenant(
                req.name(), req.mpesaShortcode(),
                req.consumerKey(), req.consumerSecret(), req.passkey()));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/admin/tenants")
    public ResponseEntity<List<Tenant>> listTenants() {
        return ResponseEntity.ok(tenantService.listTenants());
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN')")
    @PostMapping("/tenant/users")
    public ResponseEntity<AppUser> createUser(@Valid @RequestBody CreateUserRequest req,
                                              @AuthenticationPrincipal AppUser caller) {
        Long tenantId = caller.getRole() == Role.SUPER_ADMIN ? req.tenantId() : caller.getTenantId();
        AppUser user = tenantService.createUser(req.username(), req.password(), req.role(), tenantId, req.stageId());
        user.setPassword("[HIDDEN]");
        return ResponseEntity.ok(user);
    }

    // ── Stages ────────────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
    @PostMapping("/tenant/stages")
    public ResponseEntity<Stage> createStage(@Valid @RequestBody CreateStageRequest req,
                                             @AuthenticationPrincipal AppUser caller) {
        Long tenantId = caller.getRole() == Role.SUPER_ADMIN ? req.tenantId() : caller.getTenantId();
        return ResponseEntity.ok(tenantService.createStage(tenantId, req.name(), req.location()));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN','STAGE_HEAD','STAGE_ATTENDANT')")
    @GetMapping("/tenant/stages")
    public ResponseEntity<List<Stage>> listStages(@AuthenticationPrincipal AppUser caller) {
        return ResponseEntity.ok(tenantService.listStages(caller.getTenantId()));
    }

    // ── Trips ─────────────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
    @PostMapping("/tenant/trips")
    public ResponseEntity<Trip> createTrip(@Valid @RequestBody CreateTripRequest req,
                                           @AuthenticationPrincipal AppUser caller) {
        Long tenantId = caller.getRole() == Role.SUPER_ADMIN ? req.tenantId() : caller.getTenantId();
        return ResponseEntity.ok(tenantService.createTrip(
                tenantId, req.fromStageId(), req.toDestination(),
                req.route(), req.departureTime(), req.totalSeats(), req.basePrice()));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN','STAGE_HEAD','STAGE_ATTENDANT')")
    @GetMapping("/tenant/trips")
    public ResponseEntity<List<Trip>> listTrips(@AuthenticationPrincipal AppUser caller) {
        return ResponseEntity.ok(tenantService.listTrips(caller.getTenantId()));
    }

    // ── Fares (Dynamic Pricing) ───────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN','STAGE_HEAD')")
    @PostMapping("/tenant/trips/{tripId}/fares")
    public ResponseEntity<Fare> createFare(@PathVariable Long tripId,
                                           @Valid @RequestBody CreateFareRequest req,
                                           @AuthenticationPrincipal AppUser caller) {
        return ResponseEntity.ok(tenantService.createFare(
                tripId, req.effectiveFrom(), req.effectiveTo(), req.price(), caller.getId()));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN','STAGE_HEAD','STAGE_ATTENDANT')")
    @GetMapping("/tenant/trips/{tripId}/fares")
    public ResponseEntity<List<Fare>> listFares(@PathVariable Long tripId) {
        return ResponseEntity.ok(tenantService.listFares(tripId));
    }

    // ── Vehicles ──────────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN','STAGE_HEAD')")
    @PostMapping("/stage/vehicles")
    public ResponseEntity<Vehicle> createVehicle(@Valid @RequestBody CreateVehicleRequest req,
                                                 @AuthenticationPrincipal AppUser caller) {
        Long stageId = caller.getStageId() != null ? caller.getStageId() : req.stageId();
        if (stageId == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(tenantService.createVehicle(stageId, req.registrationNumber(), req.capacity()));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN','STAGE_HEAD','STAGE_ATTENDANT')")
    @GetMapping("/stage/vehicles")
    public ResponseEntity<List<Vehicle>> listVehicles(@AuthenticationPrincipal AppUser caller) {
        if (caller.getStageId() == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(tenantService.listVehicles(caller.getStageId()));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN','STAGE_HEAD')")
    @PatchMapping("/stage/vehicles/{id}/toggle")
    public ResponseEntity<Vehicle> toggleVehicle(@PathVariable Long id,
                                                 @RequestBody Map<String, Boolean> body) {
        Boolean active = body.get("active");
        if (active == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(tenantService.toggleVehicle(id, active));
    }
}
