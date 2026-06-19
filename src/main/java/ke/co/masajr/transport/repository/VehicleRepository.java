package ke.co.masajr.transport.repository;

import ke.co.masajr.transport.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByStageId(Long stageId);
    List<Vehicle> findByStageIdAndIsActive(Long stageId, Boolean isActive);
    long countByStageIdAndIsActive(Long stageId, Boolean isActive);
    long countByIsActive(Boolean isActive);
    List<Vehicle> findByTenantId(Long tenantId);
    List<Vehicle> findByTenantIdAndRegistrationNumberContainingIgnoreCase(Long tenantId, String reg);
    List<Vehicle> findByRegistrationNumberContainingIgnoreCase(String reg);
}
