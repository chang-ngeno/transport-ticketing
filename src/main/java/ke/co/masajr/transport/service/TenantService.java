package ke.co.masajr.transport.service;

import ke.co.masajr.transport.entity.*;
import ke.co.masajr.transport.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;

@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    // Keep repositories only for lightweight read usages left (if any)
    private final TenantRepository tenantRepository;
    private final StageRepository stageRepository;
    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;

    // New domain services (extracted responsibilities)
    private final TenantManagementService tenantManagementService;
    private final UserAccountService userAccountService;
    private final StageService stageService;
    private final TripService tripService;
    private final FareService fareService;
    private final VehicleService vehicleService;

    public TenantService(TenantRepository tenantRepository,
                         StageRepository stageRepository,
                         TripRepository tripRepository,
                         VehicleRepository vehicleRepository,
                         TenantManagementService tenantManagementService,
                         UserAccountService userAccountService,
                         StageService stageService,
                         TripService tripService,
                         FareService fareService,
                         VehicleService vehicleService) {
        this.tenantRepository = tenantRepository;
        this.stageRepository = stageRepository;
        this.tripRepository = tripRepository;
        this.vehicleRepository = vehicleRepository;
        this.tenantManagementService = tenantManagementService;
        this.userAccountService = userAccountService;
        this.stageService = stageService;
        this.tripService = tripService;
        this.fareService = fareService;
        this.vehicleService = vehicleService;
    }

    // ── Tenant ────────────────────────────────────────────────────────────────

    @Transactional
    public Tenant createTenant(String name, String shortcode,
                               String consumerKey, String consumerSecret, String passkey) {
        String salt = generateSalt();
        return tenantManagementService.createTenant(name, shortcode, consumerKey, consumerSecret, passkey, salt);
    }

    public List<Tenant> listTenants() {
        log.debug("Listing all tenants");
        return tenantManagementService.listTenants();
    }

    @Transactional
    public Tenant updateTenant(Long id, String name, String shortcode) {
        return tenantManagementService.updateTenant(id, name, shortcode);
    }

    @Transactional
    public void deleteTenant(Long id) {
        log.warn("Deleting tenant id={}", id);
        tenantManagementService.deleteTenant(id);
        log.info("Tenant id={} deleted", id);
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    @Transactional
    public AppUser createUser(String username, String password, Role role,
                              Long tenantId, Long stageId) {
        return userAccountService.createUser(username, password, role, tenantId, stageId);
    }

    // ── Stages ────────────────────────────────────────────────────────────────

    @Transactional
    public Stage createStage(Long tenantId, String name, String location) {
        return stageService.createStage(tenantId, name, location);
    }

    public List<AppUser> listUsers() {
        return userAccountService.listUsers();
    }

    @Transactional
    public AppUser updateUser(Long id, String username, String password, Long tenantId) {
        return userAccountService.updateUser(id, username, password, tenantId);
    }

    public List<Stage> listStages(Long tenantId) {
        return stageService.listStages(tenantId);
    }

    // ── Trips ─────────────────────────────────────────────────────────────────

    @Transactional
    public Trip createTrip(Long tenantId, Long fromStageId, String toDestination,
                           String route, java.time.LocalDateTime departureTime,
                           int totalSeats, java.math.BigDecimal basePrice) {
        return tripService.createTrip(tenantId, fromStageId, toDestination, route, departureTime, totalSeats, basePrice);
    }

    public List<Trip> listTrips(Long tenantId) {
        return tripService.listTrips(tenantId);
    }

    // ── Fares (Dynamic Pricing) ───────────────────────────────────────────────

    @Transactional
    public Fare createFare(Long tripId, java.time.LocalDateTime effectiveFrom,
                           java.time.LocalDateTime effectiveTo,
                           java.math.BigDecimal price, Long createdBy) {
        return fareService.createFare(tripId, effectiveFrom, effectiveTo, price, createdBy);
    }

    public List<Fare> listFares(Long tripId) {
        return fareService.listFares(tripId);
    }

    // ── Vehicles ──────────────────────────────────────────────────────────────

    @Transactional
    public Vehicle createVehicle(Long stageId, String registrationNumber, int capacity) {
        return vehicleService.createVehicle(stageId, registrationNumber, capacity);
    }

    public List<Vehicle> listAllVehicles() {
        return vehicleService.listAllVehicles();
    }

    public List<Vehicle> listVehicles(Long stageId) {
        return vehicleService.listVehicles(stageId);
    }

    @Transactional
    public Vehicle toggleVehicle(Long vehicleId, boolean active) {
        return vehicleService.toggleVehicle(vehicleId, active);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generateSalt() {
        byte[] bytes = new byte[8];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes); // 16 hex chars
    }
}
