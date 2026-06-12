package ke.co.masajr.transport.service;

import ke.co.masajr.transport.entity.Tenant;
import ke.co.masajr.transport.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TenantManagementService {

    private static final Logger log = LoggerFactory.getLogger(TenantManagementService.class);

    private final TenantRepository tenantRepository;
    private final EncryptionService encryptionService;

    public TenantManagementService(TenantRepository tenantRepository,
                                   EncryptionService encryptionService) {
        this.tenantRepository = tenantRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional
    public Tenant createTenant(String name, String shortcode,
                               String consumerKey, String consumerSecret, String passkey,
                               String salt) {
        log.info("Creating tenant '{}' shortcode={}", name, shortcode);
        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setMpesaShortcode(shortcode);
        tenant.setMpesaEncryptionSalt(salt);
        tenant.setMpesaConsumerKeyEncrypted(encryptionService.encrypt(consumerKey, salt));
        tenant.setMpesaConsumerSecretEncrypted(encryptionService.encrypt(consumerSecret, salt));
        tenant.setMpesaPasskeyEncrypted(encryptionService.encrypt(passkey, salt));
        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant created id={} name='{}'", saved.getId(), saved.getName());
        return saved;
    }

    public List<Tenant> listTenants() {
        return tenantRepository.findAll();
    }

    @Transactional
    public Tenant updateTenant(Long id, String name, String shortcode) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + id));
        if (name != null && !name.isBlank()) tenant.setName(name);
        if (shortcode != null && !shortcode.isBlank()) tenant.setMpesaShortcode(shortcode);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public void deleteTenant(Long id) {
        tenantRepository.deleteById(id);
    }
}
