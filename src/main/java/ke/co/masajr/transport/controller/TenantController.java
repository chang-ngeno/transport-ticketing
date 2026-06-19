package ke.co.masajr.transport.controller;

import jakarta.validation.Valid;
import ke.co.masajr.transport.dto.*;
import ke.co.masajr.transport.entity.*;
import ke.co.masajr.transport.service.TenantService;
import ke.co.masajr.transport.service.TicketBookingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
public class TenantController {

    private final TenantService tenantService;
    private final TicketBookingService bookingService;

    public TenantController(TenantService tenantService, TicketBookingService bookingService) {
        this.tenantService = tenantService;
        this.bookingService = bookingService;
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
        log.debug("Listing all tenants");
        return ResponseEntity.ok(tenantService.listTenants());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/admin/tenants/{id}")
    public ResponseEntity<Tenant> updateTenant(@PathVariable Long id,
                                               @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(tenantService.updateTenant(id,
                (String) body.get("name"),
                (String) body.get("mpesaShortcode")));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/admin/tenants/{id}")
    public ResponseEntity<Void> deleteTenant(@PathVariable Long id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.noContent().build();
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

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN')")
    @GetMapping("/tenants/users")
    public ResponseEntity<List<AppUser>> listTenantUsers(@AuthenticationPrincipal AppUser caller) {
        Long tenantId = caller.getRole() == Role.SUPER_ADMIN ? null : caller.getTenantId();
        var users = tenantService.listUsers().stream()
                .filter(u -> tenantId == null || tenantId.equals(u.getTenantId()))
                .toList();
        users.forEach(u -> u.setPassword("[HIDDEN]"));
        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TENANT_ADMIN')")
    @GetMapping("/admin/users")
    public ResponseEntity<List<AppUser>> listUsers() {
        var users = tenantService.listUsers();
        users.forEach(u -> u.setPassword("[HIDDEN]"));
        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/admin/users/{id}")
    public ResponseEntity<AppUser> updateTenantAdmin(@PathVariable Long id,
                                                     @RequestBody Map<String, Object> body) {
        AppUser user = tenantService.updateUser(id,
                (String) body.get("username"),
                (String) body.get("password"),
                body.get("tenantId") != null ? Long.parseLong(body.get("tenantId").toString()) : null);
        user.setPassword("[HIDDEN]");
        return ResponseEntity.ok(user);
    }

    // ── Stages ────────────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN','STAGE_HEAD')")
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

    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN','STAGE_HEAD','STAGE_ATTENDANT')")
    @GetMapping("/tenant/vehicles/search")
    public ResponseEntity<List<Vehicle>> searchVehicles(@RequestParam(required = false) String q,
                                                        @AuthenticationPrincipal AppUser caller) {
        Long tenantId = caller.getTenantId();
        return ResponseEntity.ok(tenantService.searchVehicles(tenantId, q));
    }

    // ── Trips ─────────────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN','STAGE_HEAD')")
    @PostMapping("/tenant/trips")
    public ResponseEntity<Trip> createTrip(@Valid @RequestBody CreateTripRequest req,
                                           @AuthenticationPrincipal AppUser caller) {
        Long tenantId = caller.getRole() == Role.SUPER_ADMIN ? req.tenantId() : caller.getTenantId();
        Long restrictedStageId = (caller.getRole() == Role.STAGE_HEAD || caller.getRole() == Role.STAGE_ATTENDANT) ? caller.getStageId() : null;
        return ResponseEntity.ok(tenantService.createTrip(
            tenantId, req.fromStageId(), req.toStageId(), req.vehicleId(), req.toDestination(),
            req.route(), req.tripStartTime(), req.totalSeats(), req.basePrice(), restrictedStageId));
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
        List<Vehicle> vehicles;
        if (caller.getRole() == Role.SUPER_ADMIN) {
            vehicles = tenantService.listAllVehicles();
        } else if (caller.getStageId() != null) {
            vehicles = tenantService.listVehicles(caller.getStageId());
        } else {
            vehicles = tenantService.searchVehicles(caller.getTenantId(), null);
        }
        return ResponseEntity.ok(vehicles);
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN','STAGE_HEAD')")
    @PatchMapping("/stage/vehicles/{id}/toggle")
    public ResponseEntity<Vehicle> toggleVehicle(@PathVariable Long id,
                                                 @RequestBody Map<String, Boolean> body) {
        Boolean active = body.get("active");
        if (active == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(tenantService.toggleVehicle(id, active));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN','STAGE_HEAD')")
    @PostMapping("/tenant/trips/{id}/start")
    public ResponseEntity<Trip> startTrip(@PathVariable Long id) {
        return ResponseEntity.ok(tenantService.startTrip(id));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN','STAGE_HEAD')")
    @PostMapping("/tenant/trips/{id}/end")
    public ResponseEntity<Trip> endTrip(@PathVariable Long id) {
        return ResponseEntity.ok(tenantService.endTrip(id));
    }

    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN','STAGE_HEAD','STAGE_ATTENDANT')")
    @GetMapping("/tenant/trips/{tripId}/manifest")
    public ResponseEntity<ke.co.masajr.transport.dto.TripManifestResponse> getTripManifest(@PathVariable Long tripId) {
        return ResponseEntity.ok(bookingService.getTripManifest(tripId));
    }
}
