package ke.co.masajr.transport.repository;

import ke.co.masajr.transport.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {
    Optional<BookingEntity> findByTicketId(String ticketId);
    Optional<BookingEntity> findByCheckoutRequestId(String checkoutRequestId);
    List<BookingEntity> findByTenantId(Long tenantId);
    List<BookingEntity> findByTripId(Long tripId);
    long countByStatus(String status);
    long countByTenantId(Long tenantId);
    long countByTenantIdAndStatus(Long tenantId, String status);
    List<BookingEntity> findTop10ByOrderByCreatedAtDesc();
    List<BookingEntity> findTop10ByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
