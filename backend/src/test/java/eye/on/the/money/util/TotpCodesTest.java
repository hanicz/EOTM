package eye.on.the.money.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TotpCodesTest {

    private static final String RFC_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @ParameterizedTest
    @CsvSource({
            "59, 287082",
            "1111111109, 081804",
            "1111111111, 050471",
            "1234567890, 005924",
            "2000000000, 279037",
            "20000000000, 353130"
    })
    public void matchesTheRfc6238TestVectors(long epochSeconds, String expected) {
        assertEquals(expected, TotpCodes.codeAt(RFC_SECRET, TotpCodes.timeStep(epochSeconds)));
    }

    @Test
    public void base32EncodesTheRfcSampleKey() {
        byte[] key = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
        assertEquals(RFC_SECRET, TotpCodes.encodeBase32(key));
        assertArrayEquals(key, TotpCodes.decodeBase32(RFC_SECRET));
    }

    @Test
    public void base32RoundTripsAGeneratedSecret() {
        String secret = TotpCodes.generateSecret();
        assertEquals(32, secret.length());
        assertEquals(TotpCodes.SECRET_BYTES, TotpCodes.decodeBase32(secret).length);
        assertEquals(secret, TotpCodes.encodeBase32(TotpCodes.decodeBase32(secret)));
    }

    @Test
    public void generatedSecretsCarryNoPadding() {
        assertTrue(TotpCodes.generateSecret().chars().noneMatch(c -> c == '='));
    }

    @Test
    public void generatedSecretsDiffer() {
        assertNotEquals(TotpCodes.generateSecret(), TotpCodes.generateSecret());
    }

    @Test
    public void decodeRejectsCharactersOutsideTheAlphabet() {
        assertThrows(IllegalArgumentException.class, () -> TotpCodes.decodeBase32("NOT-BASE32!"));
    }

    @Test
    public void decodeToleratesSpacesAndPadding() {
        assertArrayEquals(TotpCodes.decodeBase32(RFC_SECRET),
                TotpCodes.decodeBase32("gezd gnbv gy3t qojq gezd gnbv gy3t qojq=="));
    }

    @Test
    public void everyCodeIsSixDigits() {
        for (long step = 0; step < 200; step++) {
            String code = TotpCodes.codeAt(RFC_SECRET, step);
            assertEquals(TotpCodes.DIGITS, code.length(), "step " + step);
            assertTrue(code.chars().allMatch(Character::isDigit), "step " + step);
        }
    }

    @Test
    public void timeStepAdvancesEveryThirtySeconds() {
        assertEquals(0, TotpCodes.timeStep(0));
        assertEquals(0, TotpCodes.timeStep(29));
        assertEquals(1, TotpCodes.timeStep(30));
        assertEquals(1, TotpCodes.timeStep(59));
        assertEquals(2, TotpCodes.timeStep(60));
    }

    @Test
    public void matchingStepAcceptsTheNeighbouringSteps() {
        long current = 1000;
        assertEquals(current - 1, TotpCodes.matchingStep(RFC_SECRET, TotpCodes.codeAt(RFC_SECRET, current - 1), current, 1));
        assertEquals(current, TotpCodes.matchingStep(RFC_SECRET, TotpCodes.codeAt(RFC_SECRET, current), current, 1));
        assertEquals(current + 1, TotpCodes.matchingStep(RFC_SECRET, TotpCodes.codeAt(RFC_SECRET, current + 1), current, 1));
    }

    @Test
    public void matchingStepRejectsCodesOutsideTheWindow() {
        long current = 1000;
        assertEquals(TotpCodes.NO_MATCH, TotpCodes.matchingStep(RFC_SECRET, TotpCodes.codeAt(RFC_SECRET, current - 2), current, 1));
        assertEquals(TotpCodes.NO_MATCH, TotpCodes.matchingStep(RFC_SECRET, TotpCodes.codeAt(RFC_SECRET, current + 2), current, 1));
    }

    @Test
    public void matchingStepIgnoresSpacesInTheSubmittedCode() {
        long current = 1000;
        String code = TotpCodes.codeAt(RFC_SECRET, current);
        String spaced = code.substring(0, 3) + " " + code.substring(3);
        assertEquals(current, TotpCodes.matchingStep(RFC_SECRET, spaced, current, 1));
    }

    @Test
    public void matchingStepRejectsMalformedCodes() {
        assertEquals(TotpCodes.NO_MATCH, TotpCodes.matchingStep(RFC_SECRET, null, 1000, 1));
        assertEquals(TotpCodes.NO_MATCH, TotpCodes.matchingStep(RFC_SECRET, "", 1000, 1));
        assertEquals(TotpCodes.NO_MATCH, TotpCodes.matchingStep(RFC_SECRET, "12345", 1000, 1));
        assertEquals(TotpCodes.NO_MATCH, TotpCodes.matchingStep(RFC_SECRET, "1234567", 1000, 1));
    }

    @Test
    public void normalizeStripsSpaces() {
        assertEquals("123456", TotpCodes.normalize(" 123 456 "));
        assertEquals("", TotpCodes.normalize(null));
    }
}
