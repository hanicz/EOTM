package eye.on.the.money.service.financial;

import eye.on.the.money.model.financial.AccountSide;
import eye.on.the.money.model.financial.BankExclusionRule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record ExclusionRuleMatcher(Set<String> ownAccounts, Set<String> partnerAccounts) {

    private static final ExclusionRuleMatcher EMPTY = new ExclusionRuleMatcher(Set.of(), Set.of());

    private static final int IBAN_PREFIX_LENGTH = 4;
    private static final int IBAN_MIN_LENGTH = 15;
    private static final int IBAN_MAX_LENGTH = 34;

    private static final int BBAN_SHORT_LENGTH = 16;
    private static final int BBAN_LONG_LENGTH = 24;
    private static final String EMPTY_BBAN_BLOCK = "00000000";

    public static String normalize(String accountNumber) {
        return trimEmptyBbanBlock(stripIbanPrefix(compact(accountNumber)));
    }

    private static String compact(String accountNumber) {
        if (accountNumber == null) {
            return "";
        }
        StringBuilder compacted = new StringBuilder(accountNumber.length());
        for (char character : accountNumber.toCharArray()) {
            if (Character.isLetterOrDigit(character)) {
                compacted.append(Character.toUpperCase(character));
            }
        }
        return compacted.toString();
    }

    private static String stripIbanPrefix(String compacted) {
        if (compacted.length() < IBAN_MIN_LENGTH || compacted.length() > IBAN_MAX_LENGTH) {
            return compacted;
        }
        if (!Character.isLetter(compacted.charAt(0)) || !Character.isLetter(compacted.charAt(1))
                || !Character.isDigit(compacted.charAt(2)) || !Character.isDigit(compacted.charAt(3))) {
            return compacted;
        }
        return compacted.substring(IBAN_PREFIX_LENGTH);
    }

    private static String trimEmptyBbanBlock(String compacted) {
        if (compacted.length() != BBAN_LONG_LENGTH || !compacted.endsWith(EMPTY_BBAN_BLOCK)) {
            return compacted;
        }
        for (int index = 0; index < BBAN_SHORT_LENGTH; index++) {
            if (!Character.isDigit(compacted.charAt(index))) {
                return compacted;
            }
        }
        return compacted.substring(0, BBAN_SHORT_LENGTH);
    }

    public static ExclusionRuleMatcher empty() {
        return EMPTY;
    }

    public static ExclusionRuleMatcher of(List<BankExclusionRule> rules) {
        if (rules.isEmpty()) {
            return EMPTY;
        }
        Set<String> own = new HashSet<>();
        Set<String> partner = new HashSet<>();
        for (BankExclusionRule rule : rules) {
            if (rule.getNormalizedAccount() == null || rule.getNormalizedAccount().isEmpty()) {
                continue;
            }
            if (rule.getSide() == AccountSide.OWN_ACCOUNT || rule.getSide() == AccountSide.ANY) {
                own.add(rule.getNormalizedAccount());
            }
            if (rule.getSide() == AccountSide.PARTNER_ACCOUNT || rule.getSide() == AccountSide.ANY) {
                partner.add(rule.getNormalizedAccount());
            }
        }
        return new ExclusionRuleMatcher(Set.copyOf(own), Set.copyOf(partner));
    }

    public boolean matches(String accountNumber, String partnerAccount) {
        return this.contains(this.ownAccounts, accountNumber)
                || this.contains(this.partnerAccounts, partnerAccount);
    }

    private boolean contains(Set<String> accounts, String value) {
        if (accounts.isEmpty()) {
            return false;
        }
        String normalized = normalize(value);
        return !normalized.isEmpty() && accounts.contains(normalized);
    }
}
