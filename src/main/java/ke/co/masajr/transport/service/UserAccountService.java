package ke.co.masajr.transport.service;

import ke.co.masajr.transport.entity.AppUser;
import ke.co.masajr.transport.entity.Role;
import ke.co.masajr.transport.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAccountService {

    private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser createUser(String username, String password, Role role,
                              Long tenantId, Long stageId) {
        log.info("Creating user '{}' role={} tenantId={} stageId={}", username, role, tenantId, stageId);
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setTenantId(tenantId);
        user.setStageId(stageId);
        AppUser saved = userRepository.save(user);
        log.info("User created id={} username='{}'", saved.getId(), saved.getUsername());
        return saved;
    }

    public List<AppUser> listUsers() {
        log.debug("Listing all users");
        return userRepository.findAll();
    }

    @Transactional
    public AppUser updateUser(Long id, String username, String password, Long tenantId) {
        log.info("Updating user id={}", id);
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        if (user.getRole() != Role.TENANT_ADMIN)
            throw new IllegalArgumentException("Only TENANT_ADMIN users can be edited here");
        if (username != null && !username.isBlank()) user.setUsername(username);
        if (password != null && !password.isBlank()) user.setPassword(passwordEncoder.encode(password));
        if (tenantId != null) user.setTenantId(tenantId);
        AppUser saved = userRepository.save(user);
        log.info("User updated id={} username='{}'", saved.getId(), saved.getUsername());
        return saved;
    }
}
