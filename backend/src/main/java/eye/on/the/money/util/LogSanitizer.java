package eye.on.the.money.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public final class LogSanitizer {

    private static final String REDACTED = "(redacted)";

    private static final String SEPARATOR = ", ";

    private LogSanitizer() {
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return REDACTED;
        }
        int at = email.indexOf('@');
        if (at < 1) {
            return REDACTED;
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    public static String maskEmails(Collection<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return REDACTED;
        }
        return emails.stream().map(LogSanitizer::maskEmail).collect(Collectors.joining(SEPARATOR));
    }

    public static String maskEmails(String[] emails) {
        return emails == null ? REDACTED : maskEmails(Arrays.asList(emails));
    }
}
