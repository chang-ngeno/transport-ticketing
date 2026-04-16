package ke.co.masajr.transport.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant")
@Data
public class Tenant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "mpesa_shortcode", nullable = false, length = 20)
    private String mpesaShortcode;

    @Column(name = "mpesa_consumer_key_encrypted")
    private String mpesaConsumerKeyEncrypted;

    @Column(name = "mpesa_consumer_secret_encrypted")
    private String mpesaConsumerSecretEncrypted;

    @Column(name = "mpesa_passkey_encrypted")
    private String mpesaPasskeyEncrypted;

    @Column(name = "mpesa_encryption_salt", nullable = false, length = 16)
    private String mpesaEncryptionSalt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
