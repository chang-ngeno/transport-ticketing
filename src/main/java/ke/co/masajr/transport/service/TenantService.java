package ke.co.masajr.transport.service;

import ke.co.masajr.transport.entity.*;
import ke.co.masajr.transport.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository userRepository;
    private final StageRepository stageRepository;
    private final TripRepository tripRepository;
    private final FareRepository fareRepository;
    private final VehicleRepository vehicleRepository;
    private final EncryptionService encryptionService;
    private final PasswordEncoder passwordEncoder;

    public TenantService(TenantRepository tenantRepository,
                         AppUserRepository userRepository,
                         StageRepository stageRepository,
                         TripRepository tripRepository,
                         FareRepository fareRepository,
                         VehicleRepository vehicleRepository,
                         EncryptionService encryptionService,
                         PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.stageRepository = stageRepository;
        this.tripRepository = tripRepository;
        this.fareRepository = fareRepository;
        this.vehicleRepository = vehicleRepository;
        this.encryptionService = encryptionService;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Tenant ────────────────────────────────────────────────────────────────

    @Transactional
    public Tenant createTenant(String name, String shortcode,
                               String consumerKey, String consumerSecret, String passkey) {
        String salt = generateSalt();
        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setMpesaShortcode(shortcode);
        tenant.setMpesaEncryptionSalt(salt);
        tenant.setMpesaConsumerKeyEncrypted(encryptionService.encrypt(consumerKey, salt));
        tenant.setMpesaConsumerSecretEncrypted(encryptionService.encrypt(consumerSecret, salt));
        tenant.setMpesaPasskeyEncrypted(encryptionService.encrypt(passkey, salt));
        return tenantRepository.save(tenant);
    }

    public List<Tenant> listTenants() {
        return tenantRepository.findAll();
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    @Transactional
    public AppUser createUser(String username, String password, Role role,
                              Long tenantId, Long stageId) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setTenantId(tenantId);
        user.setStageId(stageId);
        return userRepository.save(user);
    }

    // ── Stages ────────────────────────────────────────────────────────────────

    @Transactional
    public Stage createStage(Long tenantId, String name, String location) {
        Stage stage = new Stage();
        stage.setTenantId(tenantId);
        stage.setName(name);
        stage.setLocation(location);
        return stageRepository.save(stage);
    }

    public List<Stage> listStages(Long tenantId) {
        return stageRepository.findByTenantId(tenantId);
    }

    // ── Trips ─────────────────────────────────────────────────────────────────

    @Transactional
    public Trip createTrip(Long tenantId, Long fromStageId, String toDestination,
                           String route, java.time.LocalDateTime departureTime,
                           int totalSeats, java.math.BigDecimal basePrice) {
        Trip trip = new Trip();
        trip.setTenantId(tenantId);
        trip.setFromStageId(fromStageId);
        trip.setToDestination(toDestination);
        trip.setRoute(route);
        trip.setDepartureTime(departureTime);
        trip.setTotalSeats(totalSeats);
        trip.setBookedSeats(0);
        trip.setPricePerSeat(basePrice);
        return tripRepository.save(trip);
    }

    public List<Trip> listTrips(Long tenantId) {
        return tripRepository.findByTenantId(tenantId);
    }

    // ── Fares (Dynamic Pricing) ───────────────────────────────────────────────

    @Transactional
    public Fare createFare(Long tripId, java.time.LocalDateTime effectiveFrom,
                           java.time.LocalDateTime effectiveTo,
                           java.math.BigDecimal price, Long createdBy) {
        Fare fare = new Fare();
        fare.setTripId(tripId);
        fare.setEffectiveFrom(effectiveFrom);
        fare.setEffectiveTo(effectiveTo);
        fare.setPricePerSeat(price);
        fare.setCreatedBy(createdBy);
        return fareRepository.save(fare);
    }

    public List<Fare> listFares(Long tripId) {
        return fareRepository.findByTripIdOrderByEffectiveFromDesc(tripId);
    }

    // ── Vehicles ──────────────────────────────────────────────────────────────

    @Transactional
    public Vehicle createVehicle(Long stageId, String registrationNumber, int capacity) {
        Vehicle vehicle = new Vehicle();
        vehicle.setStageId(stageId);
        vehicle.setRegistrationNumber(registrationNumber);
        vehicle.setCapacity(capacity);
        vehicle.setIsActive(true);
        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> listVehicles(Long stageId) {
        return vehicleRepository.findByStageId(stageId);
    }

    @Transactional
    public Vehicle toggleVehicle(Long vehicleId, boolean active) {
        Vehicle v = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + vehicleId));
        v.setIsActive(active);
        return vehicleRepository.save(v);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generateSalt() {
        byte[] bytes = new byte[8];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes); // 16 hex chars
    }
}
