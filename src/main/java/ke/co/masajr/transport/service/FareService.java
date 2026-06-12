package ke.co.masajr.transport.service;

import ke.co.masajr.transport.entity.Fare;
import ke.co.masajr.transport.repository.FareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FareService {

    private static final Logger log = LoggerFactory.getLogger(FareService.class);

    private final FareRepository fareRepository;

    public FareService(FareRepository fareRepository) {
        this.fareRepository = fareRepository;
    }

    @Transactional
    public Fare createFare(Long tripId, LocalDateTime effectiveFrom,
                           LocalDateTime effectiveTo, BigDecimal price, Long createdBy) {
        log.info("Creating fare tripId={} from={} to={} price={}", tripId, effectiveFrom, effectiveTo, price);
        Fare fare = new Fare();
        fare.setTripId(tripId);
        fare.setEffectiveFrom(effectiveFrom);
        fare.setEffectiveTo(effectiveTo);
        fare.setPricePerSeat(price);
        fare.setCreatedBy(createdBy);
        Fare saved = fareRepository.save(fare);
        log.info("Fare created id={} tripId={}", saved.getId(), saved.getTripId());
        return saved;
    }

    public List<Fare> listFares(Long tripId) {
        log.debug("Listing fares tripId={}", tripId);
        return fareRepository.findByTripIdOrderByEffectiveFromDesc(tripId);
    }
}
