package ke.co.masajr.transport.repository;

import ke.co.masajr.transport.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StageRepository extends JpaRepository<Stage, Long> {
    List<Stage> findByTenantId(Long tenantId);
}
