package eye.on.the.money.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

public final class TotpCodes {

    public static final int SECRET_BYTES = 20;

    public static final int DIGITS = 6;

    public static final int PERIOD_SECONDS = 30;

    public static final long NO_MATCH = Long.MIN_VALUE;

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private static final String ALGORITHM = "HmacSHA1";

    private static final int DIVISOR = 1_000_000;

    private static final SecureRandom RANDOM = new SecureRandom();

    private TotpCodes() {
    }

    public static String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    public static long timeStep(long epochSeconds) {
        return Math.floorDiv(epochSeconds, PERIOD_SECONDS);
    }

    public static String codeAt(String secret, long step) {
        byte[] counter = ByteBuffer.allocate(Long.BYTES).putLong(step).array();
        byte[] hash = hmac(decodeBase32(secret), counter);
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
        return String.format(Locale.ROOT, "%0" + DIGITS + "d", binary % DIVISOR);
    }

    public static long matchingStep(String secret, String code, long currentStep, int window) {
        String normalized = normalize(code);
        if (normalized.length() != DIGITS) {
            return NO_MATCH;
        }
        for (long step = currentStep - window; step <= currentStep + window; step++) {
            if (constantTimeEquals(codeAt(secret, step), normalized)) {
                return step;
            }
        }
        return NO_MATCH;
    }

    public static String normalize(String code) {
        return code == null ? "" : code.replace(" ", "").trim();
    }

    public static String encodeBase32(byte[] bytes) {
        StringBuilder encoded = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte current : bytes) {
            buffer = (buffer << 8) | (current & 0xFF);
            bits += 8;
            while (bits >= 5) {
                encoded.append(ALPHABET.charAt((buffer >>> (bits - 5)) & 0x1F));
                bits -= 5;
            }
        }
        if (bits > 0) {
            encoded.append(ALPHABET.charAt((buffer << (5 - bits)) & 0x1F));
        }
        return encoded.toString();
    }

    public static byte[] decodeBase32(String secret) {
        String cleaned = secret.replace(" ", "").replace("=", "").toUpperCase(Locale.ROOT);
        ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (int i = 0; i < cleaned.length(); i++) {
            int value = ALPHABET.indexOf(cleaned.charAt(i));
            if (value < 0) {
                throw new IllegalArgumentException("Not a base32 secret");
            }
            buffer = (buffer << 5) | value;
            bits += 5;
            if (bits >= 8) {
                decoded.write((buffer >>> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return decoded.toByteArray();
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not compute the TOTP code", e);
        }
    }
}
