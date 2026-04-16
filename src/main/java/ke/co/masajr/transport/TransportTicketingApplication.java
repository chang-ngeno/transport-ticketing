package ke.co.masajr.transport;

import ke.co.masajr.transport.entity.AppUser;
import ke.co.masajr.transport.entity.Role;
import ke.co.masajr.transport.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

@SpringBootApplication
public class TransportTicketingApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransportTicketingApplication.class, args);
    }

    @Bean
    CommandLineRunner seedSuperAdmin(AppUserRepository userRepository,
                                     PasswordEncoder encoder,
                                     @Value("${superadmin.username:superadmin}") String username,
                                     @Value("${superadmin.password:}") String password) {
        return args -> {
            if (userRepository.findByUsername(username).isEmpty()) {
                if (password.isBlank()) throw new IllegalStateException("SUPERADMIN_PASSWORD is required!");
                AppUser superAdmin = new AppUser();
                superAdmin.setUsername(username);
                superAdmin.setPassword(encoder.encode(password));
                superAdmin.setRole(Role.SUPER_ADMIN);
                superAdmin.setTenantId(null);
                userRepository.save(superAdmin);
                System.out.println("✅ SUPER ADMIN CREATED: " + username);
            }
        };
    }
}
