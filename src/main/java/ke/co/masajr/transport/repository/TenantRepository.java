package ke.co.masajr.transport.repository;

import ke.co.masajr.transport.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
}
