package ke.co.masajr.transport.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/mpesa/callback").permitAll()
                .requestMatchers("/", "/login", "/offline").permitAll()
                .requestMatchers("/css/**", "/js/**", "/icons/**", "/images/**", "/manifest.json", "/sw.js", "/favicon.ico").permitAll()
                .requestMatchers("/.well-known/**").permitAll()
                // UI routes access control
                .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/tenant/users/**").hasAnyRole("TENANT_ADMIN", "SUPER_ADMIN")
                .requestMatchers("/tenant/**").hasAnyRole("TENANT_ADMIN", "SUPER_ADMIN", "STAGE_HEAD")
                .requestMatchers("/api/admin/tenants/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/tenant/users/**").hasAnyRole("TENANT_ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/tenant/**").hasAnyRole("TENANT_ADMIN", "SUPER_ADMIN", "STAGE_HEAD", "STAGE_ATTENDANT")
                .requestMatchers("/api/stage/**").hasAnyRole("STAGE_HEAD", "TENANT_ADMIN", "SUPER_ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(e -> e.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
