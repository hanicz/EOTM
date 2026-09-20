package eye.on.the.money.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TotpSecretEncryptorTest {

    private static final String SALT = "7b5d41c9a6e230f8";

    private static final String SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    private TotpSecretEncryptor encryptor;

    @BeforeEach
    public void setUp() {
        this.encryptor = new TotpSecretEncryptor("a-test-totp-key", SALT);
        this.encryptor.init();
    }

    @Test
    public void roundTripsASecret() {
        assertEquals(SECRET, this.encryptor.decrypt(this.encryptor.encrypt(SECRET)));
    }

    @Test
    public void cipherTextDiffersFromThePlainText() {
        assertNotEquals(SECRET, this.encryptor.encrypt(SECRET));
    }

    @Test
    public void theSameSecretEncryptsDifferentlyEachTime() {
        assertNotEquals(this.encryptor.encrypt(SECRET), this.encryptor.encrypt(SECRET));
    }

    @Test
    public void anotherKeyCannotDecrypt() {
        TotpSecretEncryptor other = new TotpSecretEncryptor("a-different-totp-key", SALT);
        other.init();
        String cipherText = this.encryptor.encrypt(SECRET);

        assertThrows(RuntimeException.class, () -> other.decrypt(cipherText));
    }

    @Test
    public void reportsEnabledWhenAKeyIsConfigured() {
        assertTrue(this.encryptor.isEnabled());
    }

    @Test
    public void withNeitherKeyNorSaltItStaysDisabledAndRefusesToEncrypt() {
        TotpSecretEncryptor unconfigured = new TotpSecretEncryptor("  ", "");
        unconfigured.init();

        assertFalse(unconfigured.isEnabled());
        assertThrows(IllegalStateException.class, () -> unconfigured.encrypt(SECRET));
        assertThrows(IllegalStateException.class, () -> unconfigured.decrypt("whatever"));
    }

    @Test
    public void aHalfConfiguredPairIsRefused() {
        assertThrows(IllegalStateException.class, () -> new TotpSecretEncryptor("a-test-totp-key", "").init());
        assertThrows(IllegalStateException.class, () -> new TotpSecretEncryptor("", SALT).init());
    }

    @Test
    public void aSaltThatIsNotHexIsRefused() {
        assertThrows(IllegalStateException.class,
                () -> new TotpSecretEncryptor("a-test-totp-key", "not-hexadecimal!").init());
    }

    @Test
    public void aSaltThatIsTooShortOrOddIsRefused() {
        assertThrows(IllegalStateException.class, () -> new TotpSecretEncryptor("a-test-totp-key", "7b5d41c9").init());
        assertThrows(IllegalStateException.class,
                () -> new TotpSecretEncryptor("a-test-totp-key", "7b5d41c9a6e230f8a").init());
    }

    @Test
    public void anotherSaltCannotDecrypt() {
        TotpSecretEncryptor other = new TotpSecretEncryptor("a-test-totp-key", "0011223344556677");
        other.init();
        String cipherText = this.encryptor.encrypt(SECRET);

        assertThrows(RuntimeException.class, () -> other.decrypt(cipherText));
    }
}
