package ke.co.masajr.transport.service;

import ke.co.masajr.transport.entity.Vehicle;
import ke.co.masajr.transport.entity.Stage;
import ke.co.masajr.transport.repository.VehicleRepository;
import ke.co.masajr.transport.repository.StageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.util.List;

@Service
public class VehicleService {

    private static final Logger log = LoggerFactory.getLogger(VehicleService.class);

    private final VehicleRepository vehicleRepository;
    private final StageRepository stageRepository;
    private final EntityManager entityManager;

    public VehicleService(VehicleRepository vehicleRepository, StageRepository stageRepository, EntityManager entityManager) {
        this.vehicleRepository = vehicleRepository;
        this.stageRepository = stageRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public Vehicle createVehicle(Long stageId, String registrationNumber, int capacity) {
        log.info("Registering vehicle '{}' stageId={} capacity={}", registrationNumber, stageId, capacity);
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + stageId));
        
        log.debug("Fetched stage id={} tenantId={}", stage.getId(), stage.getTenantId());
        
        Vehicle vehicle = new Vehicle();
        vehicle.setStageId(stageId);
        vehicle.setTenantId(stage.getTenantId());
        vehicle.setRegistrationNumber(registrationNumber);
        vehicle.setCapacity(capacity);
        vehicle.setIsActive(true);
        
        log.debug("Vehicle before save: tenantId={}", vehicle.getTenantId());
        
        Vehicle saved = vehicleRepository.save(vehicle);
        entityManager.flush();
        
        log.info("Vehicle registered id={} reg='{}' tenantId={}", saved.getId(), saved.getRegistrationNumber(), saved.getTenantId());
        return saved;
    }

    public List<Vehicle> listAllVehicles() {
        log.debug("Listing all vehicles");
        return vehicleRepository.findAll();
    }

    public List<Vehicle> listVehicles(Long stageId) {
        log.debug("Listing vehicles stageId={}", stageId);
        return vehicleRepository.findByStageId(stageId);
    }

    public List<Vehicle> searchByTenant(Long tenantId, String query) {
        log.debug("Searching vehicles tenantId={} query={}", tenantId, query);
        if (query == null || query.isBlank()) return vehicleRepository.findByTenantId(tenantId);
        return vehicleRepository.findByTenantIdAndRegistrationNumberContainingIgnoreCase(tenantId, query);
    }

    @Transactional
    public Vehicle toggleVehicle(Long vehicleId, boolean active) {
        log.info("Toggling vehicle id={} active={}", vehicleId, active);
        Vehicle v = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + vehicleId));
        v.setIsActive(active);
        return vehicleRepository.save(v);
    }
}
