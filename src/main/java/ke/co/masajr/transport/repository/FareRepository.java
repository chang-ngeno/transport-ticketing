package ke.co.masajr.transport.repository;

import ke.co.masajr.transport.entity.Fare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FareRepository extends JpaRepository<Fare, Long> {

    List<Fare> findByTripIdOrderByEffectiveFromDesc(Long tripId);

    /**
     * Resolves the active fare for a trip at a specific datetime.
     * Picks the most recently started fare whose window covers the given time.
     */
    @Query("""
        SELECT f FROM Fare f
        WHERE f.tripId = :tripId
          AND f.effectiveFrom <= :at
          AND (f.effectiveTo IS NULL OR f.effectiveTo > :at)
        ORDER BY f.effectiveFrom DESC
        LIMIT 1
    """)
    Optional<Fare> findActiveFare(@Param("tripId") Long tripId, @Param("at") LocalDateTime at);
}
