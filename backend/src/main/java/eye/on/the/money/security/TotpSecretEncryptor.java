package eye.on.the.money.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
public class TotpSecretEncryptor {

    private static final int MIN_SALT_LENGTH = 16;

    private static final Pattern SALT_PATTERN = Pattern.compile("([0-9a-fA-F]{2}){" + (MIN_SALT_LENGTH / 2) + ",}");

    private final String key;

    private final String salt;

    private TextEncryptor encryptor;

    public TotpSecretEncryptor(@Value("${totp.key:}") String key, @Value("${totp.salt:}") String salt) {
        this.key = key;
        this.salt = salt;
    }

    @PostConstruct
    public void init() {
        boolean hasKey = this.key != null && !this.key.isBlank();
        boolean hasSalt = this.salt != null && !this.salt.isBlank();

        if (!hasKey && !hasSalt) {
            log.warn("EOTM_TOTP_KEY and EOTM_TOTP_SALT are not set, two-factor authentication cannot be enabled");
            return;
        }
        if (!hasKey) {
            throw new IllegalStateException("EOTM_TOTP_SALT is set but EOTM_TOTP_KEY is missing");
        }
        if (!hasSalt) {
            throw new IllegalStateException("EOTM_TOTP_KEY is set but EOTM_TOTP_SALT is missing");
        }
        if (!SALT_PATTERN.matcher(this.salt).matches()) {
            throw new IllegalStateException("EOTM_TOTP_SALT must be an even number of hexadecimal characters, "
                    + "at least " + MIN_SALT_LENGTH + " of them");
        }

        this.encryptor = Encryptors.delux(this.key, this.salt);
    }

    public boolean isEnabled() {
        return this.encryptor != null;
    }

    public String encrypt(String plainText) {
        return this.required().encrypt(plainText);
    }

    public String decrypt(String cipherText) {
        return this.required().decrypt(cipherText);
    }

    private TextEncryptor required() {
        if (this.encryptor == null) {
            throw new IllegalStateException("EOTM_TOTP_KEY is not configured");
        }
        return this.encryptor;
    }
}
