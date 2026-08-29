package eye.on.the.money.service.financial;

import eye.on.the.money.model.financial.AccountSide;
import eye.on.the.money.model.financial.BankExclusionRule;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
class ExclusionRuleMatcherTest {

    private static final String OWN = "111111112222222233333333";
    private static final String PARTNER = "120010080000000000000001";

    private BankExclusionRule rule(String accountNumber, AccountSide side) {
        return BankExclusionRule.builder()
                .accountNumber(accountNumber)
                .normalizedAccount(ExclusionRuleMatcher.normalize(accountNumber))
                .side(side)
                .active(true)
                .build();
    }

    @Test
    void normalize_stripsSeparatorsAndUppercases() {
        assertEquals("111111112222", ExclusionRuleMatcher.normalize("1111-1111 2222"));
        assertEquals("AB12CD", ExclusionRuleMatcher.normalize("ab12-cd"));
    }

    @Test
    void normalize_returnsEmptyForNullBlankAndPunctuationOnly() {
        assertEquals("", ExclusionRuleMatcher.normalize(null));
        assertEquals("", ExclusionRuleMatcher.normalize("   "));
        assertEquals("", ExclusionRuleMatcher.normalize("---//---"));
    }

    @Test
    void empty_matchesNothing() {
        assertFalse(ExclusionRuleMatcher.empty().matches(OWN, PARTNER));
    }

    @Test
    void of_withNoRulesMatchesNothing() {
        assertFalse(ExclusionRuleMatcher.of(List.of()).matches(OWN, PARTNER));
    }

    @Test
    void matches_onlyTheOwnAccountForAnOwnAccountRule() {
        ExclusionRuleMatcher matcher = ExclusionRuleMatcher.of(List.of(this.rule(OWN, AccountSide.OWN_ACCOUNT)));

        assertTrue(matcher.matches(OWN, PARTNER));
        assertFalse(matcher.matches(PARTNER, OWN));
    }

    @Test
    void matches_onlyThePartnerAccountForAPartnerRule() {
        ExclusionRuleMatcher matcher = ExclusionRuleMatcher.of(List.of(this.rule(PARTNER, AccountSide.PARTNER_ACCOUNT)));

        assertTrue(matcher.matches(OWN, PARTNER));
        assertFalse(matcher.matches(PARTNER, OWN));
    }

    @Test
    void matches_eitherSideForAnAnyRule() {
        ExclusionRuleMatcher matcher = ExclusionRuleMatcher.of(List.of(this.rule(PARTNER, AccountSide.ANY)));

        assertTrue(matcher.matches(OWN, PARTNER));
        assertTrue(matcher.matches(PARTNER, OWN));
    }

    @Test
    void matches_ignoresSeparatorsAndCase() {
        ExclusionRuleMatcher matcher = ExclusionRuleMatcher.of(
                List.of(this.rule("12001008-00000000-00000001", AccountSide.PARTNER_ACCOUNT)));

        assertTrue(matcher.matches(OWN, PARTNER));
    }

    @Test
    void matches_neverOnABlankOrNullAccount() {
        ExclusionRuleMatcher matcher = ExclusionRuleMatcher.of(List.of(this.rule(PARTNER, AccountSide.ANY)));

        assertFalse(matcher.matches("", ""));
        assertFalse(matcher.matches(null, null));
        assertFalse(matcher.matches("  ", "---"));
    }

    @Test
    void of_skipsARuleWithAnEmptyNormalizedAccount() {
        ExclusionRuleMatcher matcher = ExclusionRuleMatcher.of(List.of(this.rule("---", AccountSide.ANY)));

        assertFalse(matcher.matches("---", "---"));
        assertFalse(matcher.matches(OWN, PARTNER));
    }

    @Test
    void normalize_stripsTheIbanCountryCodeAndCheckDigits() {
        assertEquals(OWN, ExclusionRuleMatcher.normalize("HU90" + OWN));
        assertEquals(OWN, ExclusionRuleMatcher.normalize("HU90 1111 1111 2222 2222 3333 3333"));
    }

    @Test
    void normalize_leavesADomesticAccountAlone() {
        assertEquals(OWN, ExclusionRuleMatcher.normalize(OWN));
        assertEquals(OWN, ExclusionRuleMatcher.normalize("11111111-22222222-33333333"));
    }

    @Test
    void normalize_leavesAShortValueThatLooksLikeAnIbanAlone() {
        assertEquals("HU90111111", ExclusionRuleMatcher.normalize("HU90-111111"));
    }

    @Test
    void normalize_leavesAValueWithoutTheIbanShapeAlone() {
        assertEquals("1111111122222222333333331234", ExclusionRuleMatcher.normalize("1111111122222222333333331234"));
        assertEquals("HUXX111111112222222233333333", ExclusionRuleMatcher.normalize("HUXX111111112222222233333333"));
    }

    @Test
    void matches_anIbanRuleAgainstADomesticAccount() {
        ExclusionRuleMatcher matcher = ExclusionRuleMatcher.of(
                List.of(this.rule("HU90" + PARTNER, AccountSide.PARTNER_ACCOUNT)));

        assertTrue(matcher.matches(OWN, PARTNER));
    }

    @Test
    void matches_aDomesticRuleAgainstAnIbanAccount() {
        ExclusionRuleMatcher matcher = ExclusionRuleMatcher.of(
                List.of(this.rule(PARTNER, AccountSide.PARTNER_ACCOUNT)));

        assertTrue(matcher.matches(OWN, "HU42" + PARTNER));
    }

    @Test
    void normalize_trimsTheEmptyThirdBlockOfADomesticAccount() {
        assertEquals("1111111122222222", ExclusionRuleMatcher.normalize("11111111-22222222-00000000"));
        assertEquals("1111111122222222", ExclusionRuleMatcher.normalize("1111111122222222"));
    }

    @Test
    void normalize_keepsAThirdBlockThatCarriesDigits() {
        assertEquals(OWN, ExclusionRuleMatcher.normalize("11111111-22222222-33333333"));
    }

    @Test
    void normalize_trimsTheEmptyThirdBlockOfAnIban() {
        assertEquals("1111111122222222", ExclusionRuleMatcher.normalize("HU90 11111111 22222222 00000000"));
    }

    @Test
    void matches_theShortAndPaddedFormsOfOneAccount() {
        ExclusionRuleMatcher matcher = ExclusionRuleMatcher.of(
                List.of(this.rule("11111111-22222222", AccountSide.PARTNER_ACCOUNT)));

        assertTrue(matcher.matches(OWN, "11111111-22222222-00000000"));
        assertTrue(matcher.matches(OWN, "HU28111111112222222200000000"));
        assertTrue(matcher.matches(OWN, "1111111122222222"));
    }

    @Test
    void matches_treatsAPaddedRuleAndAShortAccountAsOne() {
        ExclusionRuleMatcher matcher = ExclusionRuleMatcher.of(
                List.of(this.rule("111111112222222200000000", AccountSide.PARTNER_ACCOUNT)));

        assertTrue(matcher.matches(OWN, "11111111-22222222"));
    }

    @Test
    void matches_doesNotConflateAccountsThatOnlyShareTheirFirstBlocks() {
        ExclusionRuleMatcher matcher = ExclusionRuleMatcher.of(
                List.of(this.rule("11111111-22222222", AccountSide.PARTNER_ACCOUNT)));

        assertFalse(matcher.matches(OWN, "11111111-22222222-33333333"));
    }
}
