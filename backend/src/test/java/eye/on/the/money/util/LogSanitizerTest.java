package eye.on.the.money.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogSanitizerTest {

    @Test
    void maskEmail_keepsOnlyTheFirstCharacterAndTheDomain() {
        assertEquals("j***@example.com", LogSanitizer.maskEmail("jane.doe@example.com"));
    }

    @Test
    void maskEmail_keepsTheDomainOfAShortLocalPart() {
        assertEquals("a***@example.org", LogSanitizer.maskEmail("a@example.org"));
    }

    @Test
    void maskEmails_masksEveryAddressInACollection() {
        assertEquals("a***@example.com, b***@example.org",
                LogSanitizer.maskEmails(List.of("alice@example.com", "bob@example.org")));
    }

    @Test
    void maskEmails_redactsAnEmptyOrNullCollection() {
        assertEquals("(redacted)", LogSanitizer.maskEmails(List.of()));
        assertEquals("(redacted)", LogSanitizer.maskEmails((String[]) null));
    }

    @Test
    void maskEmails_masksAnArray() {
        assertEquals("c***@example.net", LogSanitizer.maskEmails(new String[]{"carol@example.net"}));
    }

    @Test
    void maskEmail_redactsNull() {
        assertEquals("(redacted)", LogSanitizer.maskEmail(null));
    }

    @Test
    void maskEmail_redactsBlank() {
        assertEquals("(redacted)", LogSanitizer.maskEmail("   "));
    }

    @Test
    void maskEmail_redactsAValueWithoutAnAtSign() {
        assertEquals("(redacted)", LogSanitizer.maskEmail("not-an-email"));
    }

    @Test
    void maskEmail_redactsAValueStartingWithAnAtSign() {
        assertEquals("(redacted)", LogSanitizer.maskEmail("@example.com"));
    }
}
