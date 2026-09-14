package eye.on.the.money.service.financial;

import eye.on.the.money.model.financial.BankCategoryRule;
import eye.on.the.money.model.financial.SpendingCategory;

import java.util.Comparator;
import java.util.List;

public record CategoryRuleMatcher(List<BankCategoryRule> rules) {

    private static final CategoryRuleMatcher EMPTY = new CategoryRuleMatcher(List.of());

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder normalized = new StringBuilder(value.length());
        boolean pendingSpace = false;
        for (char character : value.toCharArray()) {
            if (Character.isWhitespace(character)) {
                pendingSpace = !normalized.isEmpty();
                continue;
            }
            if (pendingSpace) {
                normalized.append(' ');
                pendingSpace = false;
            }
            normalized.append(Character.toUpperCase(character));
        }
        return normalized.toString();
    }

    public static CategoryRuleMatcher empty() {
        return EMPTY;
    }

    public static CategoryRuleMatcher of(List<BankCategoryRule> rules) {
        List<BankCategoryRule> usable = rules.stream()
                .filter(rule -> rule.getNormalizedPattern() != null && !rule.getNormalizedPattern().isEmpty())
                .sorted(Comparator.comparingInt(BankCategoryRule::getPriority)
                        .thenComparing(BankCategoryRule::getNormalizedPattern))
                .toList();
        return usable.isEmpty() ? EMPTY : new CategoryRuleMatcher(usable);
    }

    public SpendingCategory match(String partnerName) {
        if (this.rules.isEmpty()) {
            return null;
        }
        String normalized = normalize(partnerName);
        if (normalized.isEmpty()) {
            return null;
        }
        for (BankCategoryRule rule : this.rules) {
            if (normalized.contains(rule.getNormalizedPattern())) {
                return rule.getCategory();
            }
        }
        return null;
    }
}
