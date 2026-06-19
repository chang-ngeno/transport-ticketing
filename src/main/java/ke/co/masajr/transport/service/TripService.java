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
                           String toDestination, String route, LocalDateTime tripStartTime,
                           int totalSeats, BigDecimal basePrice, Long restrictedStageId) {
        log.info("Creating trip tenantId={} from={} toStage={} vehicle={} to='{}' departs={}",
                tenantId, fromStageId, toStageId, vehicleId, toDestination, tripStartTime);

        // Determine effective fromStageId (stage head/attendant restriction)
        Long effectiveFromStageId = (restrictedStageId != null) ? restrictedStageId : fromStageId;

        // Validate from stage belongs to tenant
        Stage from = stageRepository.findById(effectiveFromStageId)
                .orElseThrow(() -> new IllegalArgumentException("From stage not found: " + effectiveFromStageId));
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

        // Check if the vehicle is available
        if (vehicle.getStatus() != null && !"available".equalsIgnoreCase(vehicle.getStatus())) {
            throw new IllegalStateException("Vehicle " + vehicle.getRegistrationNumber() + " is currently " + vehicle.getStatus() + " and not available.");
        }

        // Track vehicle: current stage of vehicle must match effectiveFromStageId
        if (vehicle.getStageId() != null && !effectiveFromStageId.equals(vehicle.getStageId())) {
            throw new IllegalArgumentException("Vehicle " + vehicle.getRegistrationNumber() + " is currently at stage " + vehicle.getStageId() + ", but the trip starts at stage " + effectiveFromStageId);
        }

        // Default tripStartTime
        if (tripStartTime == null) tripStartTime = LocalDateTime.now();

        Trip trip = new Trip();
        trip.setTenantId(tenantId);
        trip.setFromStageId(effectiveFromStageId);
        trip.setToStageId(toStageId);
        trip.setVehicleId(vehicleId);
        trip.setToDestination(toDestination);
        trip.setRoute(route);
        trip.setTripStartTime(tripStartTime);
        trip.setTotalSeats(vehicle.getCapacity()); // Autopopulate from vehicle capacity
        trip.setBookedSeats(0);
        trip.setPricePerSeat(basePrice);
        trip.setStatus("BOARDING"); // Starts in BOARDING status

        // Update vehicle status to boarding
        vehicle.setStatus("boarding");
        vehicleRepository.save(vehicle);

        Trip saved = tripRepository.save(trip);
        log.info("Trip created id={} to='{}' capacity={}", saved.getId(), saved.getToDestination(), saved.getTotalSeats());
        return saved;
    }

    @Transactional
    public Trip startTrip(Long tripId) {
        log.info("Starting trip id={}", tripId);
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        if (!"BOARDING".equalsIgnoreCase(trip.getStatus())) {
            throw new IllegalStateException("Only trips in BOARDING status can be started");
        }
        trip.setStatus("TRAVELLING");

        Vehicle vehicle = vehicleRepository.findById(trip.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for trip: " + tripId));
        vehicle.setStatus("travelling");
        vehicleRepository.save(vehicle);

        return tripRepository.save(trip);
    }

    @Transactional
    public Trip endTrip(Long tripId) {
        log.info("Ending trip id={}", tripId);
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        if (!"TRAVELLING".equalsIgnoreCase(trip.getStatus())) {
            throw new IllegalStateException("Only trips in TRAVELLING status can be ended");
        }
        trip.setStatus("ENDED");

        Vehicle vehicle = vehicleRepository.findById(trip.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found for trip: " + tripId));
        vehicle.setStatus("available");

        // Track vehicle: update vehicle's current stage to trip's toStageId if present
        if (trip.getToStageId() != null) {
            vehicle.setStageId(trip.getToStageId());
        }
        vehicleRepository.save(vehicle);

        return tripRepository.save(trip);
    }

    public List<Trip> listTrips(Long tenantId) {
        log.debug("Listing trips tenantId={}", tenantId);
        if (tenantId == null) {
            return tripRepository.findAll();
        }
        return tripRepository.findByTenantId(tenantId);
    }
}
