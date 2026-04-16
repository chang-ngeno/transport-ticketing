package ke.co.masajr.transport.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

@Service
public class EncryptionService {

    private final String masterPassword;

    public EncryptionService(@Value("${encryption.master-password}") String masterPassword) {
        if (masterPassword == null || masterPassword.isBlank()) {
            throw new IllegalStateException("ENCRYPTION_MASTER_PASSWORD environment variable is required!");
        }
        this.masterPassword = masterPassword;
    }

    private TextEncryptor getEncryptor(String salt) {
        if (salt == null || salt.length() != 16) {
            throw new IllegalArgumentException("Tenant encryption salt must be exactly 16 hex characters");
        }
        return Encryptors.delux(masterPassword, salt);
    }

    public String encrypt(String plainText, String salt) {
        return plainText == null ? null : getEncryptor(salt).encrypt(plainText);
    }

    public String decrypt(String encryptedText, String salt) {
        return encryptedText == null ? null : getEncryptor(salt).decrypt(encryptedText);
    }
}
