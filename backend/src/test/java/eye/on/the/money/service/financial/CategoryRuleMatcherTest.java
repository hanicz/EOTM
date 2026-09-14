package eye.on.the.money.service.financial;

import eye.on.the.money.model.financial.BankCategoryRule;
import eye.on.the.money.model.financial.CategoryColor;
import eye.on.the.money.model.financial.SpendingCategory;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@ActiveProfiles("test")
class CategoryRuleMatcherTest {

    private static final SpendingCategory GROCERIES = category(1L, "Groceries");
    private static final SpendingCategory TRAVEL = category(2L, "Travel");
    private static final SpendingCategory BILLS = category(3L, "Bills");

    private static SpendingCategory category(Long id, String name) {
        return SpendingCategory.builder().id(id).name(name).normalizedName(name.toUpperCase())
                .color(CategoryColor.BLUE).build();
    }

    private static BankCategoryRule rule(String pattern, int priority, SpendingCategory category) {
        return BankCategoryRule.builder()
                .id((long) priority)
                .pattern(pattern)
                .normalizedPattern(CategoryRuleMatcher.normalize(pattern))
                .priority(priority)
                .active(true)
                .category(category)
                .build();
    }

    @Test
    void normalize_upperCasesAndCollapsesWhitespace() {
        assertEquals("BLUE MARKET 12", CategoryRuleMatcher.normalize("  blue   market\t12  "));
    }

    @Test
    void normalize_returnsEmptyForNullAndBlank() {
        assertEquals("", CategoryRuleMatcher.normalize(null));
        assertEquals("", CategoryRuleMatcher.normalize("   "));
    }

    @Test
    void match_isCaseInsensitive() {
        CategoryRuleMatcher matcher = CategoryRuleMatcher.of(List.of(rule("bluemart", 0, GROCERIES)));
        assertSame(GROCERIES, matcher.match("BLUEMART 118 Riverton"));
    }

    @Test
    void match_ignoresTheBranchNumberAndTownAroundTheMerchant() {
        CategoryRuleMatcher matcher = CategoryRuleMatcher.of(List.of(rule("BLUEMART", 0, GROCERIES)));
        assertSame(GROCERIES, matcher.match("BLUEMART HU 207 Riverton      Riverton     HU"));
    }

    @Test
    void match_collapsesTheRunsOfSpacesTheTerminalPadsNamesWith() {
        CategoryRuleMatcher matcher = CategoryRuleMatcher.of(List.of(rule("GREEN CROSS", 0, BILLS)));
        assertSame(BILLS, matcher.match("GREEN     CROSS PHARMACY   RIVERTON    HU"));
    }

    @Test
    void match_takesTheLowerPriorityRuleWhenBothPatternsFit() {
        CategoryRuleMatcher matcher = CategoryRuleMatcher.of(List.of(
                rule("PAYGATE", 5, BILLS),
                rule("PAYGATE rail", 1, TRAVEL)));
        assertSame(TRAVEL, matcher.match("PAYGATE rail-tickets   Riverton   HU"));
        assertSame(BILLS, matcher.match("PAYGATE utilities      Riverton   HU"));
    }

    @Test
    void match_returnsNullWhenNothingFits() {
        CategoryRuleMatcher matcher = CategoryRuleMatcher.of(List.of(rule("BLUEMART", 0, GROCERIES)));
        assertNull(matcher.match("CORNER BAKERY   RIVERTON   HU"));
    }

    @Test
    void match_returnsNullForABlankPartnerName() {
        CategoryRuleMatcher matcher = CategoryRuleMatcher.of(List.of(rule("BLUEMART", 0, GROCERIES)));
        assertNull(matcher.match("   "));
        assertNull(matcher.match(null));
    }

    @Test
    void match_returnsNullWhenThereAreNoRules() {
        assertNull(CategoryRuleMatcher.empty().match("BLUEMART 118 Riverton"));
        assertNull(CategoryRuleMatcher.of(List.of()).match("BLUEMART 118 Riverton"));
    }

    @Test
    void of_skipsRulesWithAnEmptyPattern() {
        BankCategoryRule broken = BankCategoryRule.builder().normalizedPattern("").priority(0)
                .category(GROCERIES).build();
        CategoryRuleMatcher matcher = CategoryRuleMatcher.of(List.of(broken, rule("BLUEMART", 1, GROCERIES)));
        assertEquals(1, matcher.rules().size());
        assertSame(GROCERIES, matcher.match("BLUEMART 118 Riverton"));
    }
}
