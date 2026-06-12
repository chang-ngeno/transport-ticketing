package ke.co.masajr.transport.service;

import ke.co.masajr.transport.entity.Vehicle;
import ke.co.masajr.transport.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehicleService {

    private static final Logger log = LoggerFactory.getLogger(VehicleService.class);

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public Vehicle createVehicle(Long stageId, String registrationNumber, int capacity) {
        log.info("Registering vehicle '{}' stageId={} capacity={}", registrationNumber, stageId, capacity);
        Vehicle vehicle = new Vehicle();
        vehicle.setStageId(stageId);
        vehicle.setRegistrationNumber(registrationNumber);
        vehicle.setCapacity(capacity);
        vehicle.setIsActive(true);
        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle registered id={} reg='{}'", saved.getId(), saved.getRegistrationNumber());
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

    @Transactional
    public Vehicle toggleVehicle(Long vehicleId, boolean active) {
        log.info("Toggling vehicle id={} active={}", vehicleId, active);
        Vehicle v = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + vehicleId));
        v.setIsActive(active);
        return vehicleRepository.save(v);
    }
}
