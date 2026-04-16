package ke.co.masajr.transport.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
@Data
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "stage_id")
    private Long stageId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
