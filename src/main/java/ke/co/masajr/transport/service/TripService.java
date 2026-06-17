package ke.co.masajr.transport.service;

import ke.co.masajr.transport.entity.Trip;
import ke.co.masajr.transport.entity.Stage;
import ke.co.masajr.transport.entity.Vehicle;
import ke.co.masajr.transport.repository.StageRepository;
import ke.co.masajr.transport.repository.TripRepository;
import ke.co.masajr.transport.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TripService {

    private static final Logger log = LoggerFactory.getLogger(TripService.class);

    private final TripRepository tripRepository;
    private final StageRepository stageRepository;
    private final VehicleRepository vehicleRepository;

    public TripService(TripRepository tripRepository,
                       StageRepository stageRepository,
                       VehicleRepository vehicleRepository) {
        this.tripRepository = tripRepository;
        this.stageRepository = stageRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public Trip createTrip(Long tenantId, Long fromStageId, Long toStageId, Long vehicleId,
                           String toDestination, String route, LocalDateTime departureTime,
                           int totalSeats, BigDecimal basePrice) {
        log.info("Creating trip tenantId={} from={} toStage={} vehicle={} to='{}' departs={}",
                tenantId, fromStageId, toStageId, vehicleId, toDestination, departureTime);

        // Validate from stage belongs to tenant
        Stage from = stageRepository.findById(fromStageId)
                .orElseThrow(() -> new IllegalArgumentException("From stage not found: " + fromStageId));
        if (!tenantId.equals(from.getTenantId())) {
            throw new IllegalArgumentException("From stage does not belong to tenant");
        }

        // Validate to stage if provided
        if (toStageId != null) {
            Stage to = stageRepository.findById(toStageId)
                    .orElseThrow(() -> new IllegalArgumentException("To stage not found: " + toStageId));
            if (!tenantId.equals(to.getTenantId())) {
                throw new IllegalArgumentException("To stage does not belong to tenant");
            }
        }

        // Vehicle must be selected and belong to tenant
        if (vehicleId == null) {
            throw new IllegalArgumentException("Vehicle selection is required when creating a trip");
        }
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));
        if (!tenantId.equals(vehicle.getTenantId())) {
            throw new IllegalArgumentException("Vehicle does not belong to tenant");
        }

        // Default departureTime
        if (departureTime == null) departureTime = LocalDateTime.now();

        Trip trip = new Trip();
        trip.setTenantId(tenantId);
        trip.setFromStageId(fromStageId);
        trip.setToStageId(toStageId);
        trip.setVehicleId(vehicleId);
        trip.setToDestination(toDestination);
        trip.setRoute(route);
        trip.setDepartureTime(departureTime);
        trip.setTotalSeats(totalSeats);
        trip.setBookedSeats(0);
        trip.setPricePerSeat(basePrice);
        Trip saved = tripRepository.save(trip);
        log.info("Trip created id={} to='{}'", saved.getId(), saved.getToDestination());
        return saved;
    }

    public List<Trip> listTrips(Long tenantId) {
        log.debug("Listing trips tenantId={}", tenantId);
        return tripRepository.findByTenantId(tenantId);
    }
}
