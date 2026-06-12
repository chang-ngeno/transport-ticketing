package ke.co.masajr.transport.service;

import ke.co.masajr.transport.entity.Trip;
import ke.co.masajr.transport.repository.TripRepository;
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

    public TripService(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @Transactional
    public Trip createTrip(Long tenantId, Long fromStageId, String toDestination,
                           String route, LocalDateTime departureTime,
                           int totalSeats, BigDecimal basePrice) {
        log.info("Creating trip tenantId={} from={} to='{}' departs={}", tenantId, fromStageId, toDestination, departureTime);
        Trip trip = new Trip();
        trip.setTenantId(tenantId);
        trip.setFromStageId(fromStageId);
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
