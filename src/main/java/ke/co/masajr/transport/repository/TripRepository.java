package ke.co.masajr.transport.repository;

import ke.co.masajr.transport.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByTenantId(Long tenantId);
    List<Trip> findByFromStageId(Long stageId);

    // Upcoming trips from now ordered soonest first
    List<Trip> findTop10ByDepartureTimeAfterOrderByDepartureTimeAsc(LocalDateTime now);
    List<Trip> findTop10ByTenantIdAndDepartureTimeAfterOrderByDepartureTimeAsc(Long tenantId, LocalDateTime now);

    // Count of upcoming trips
    long countByDepartureTimeAfter(LocalDateTime now);
    long countByTenantIdAndDepartureTimeAfter(Long tenantId, LocalDateTime now);
}
