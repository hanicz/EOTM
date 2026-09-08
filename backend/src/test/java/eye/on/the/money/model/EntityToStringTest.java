package eye.on.the.money.model;

import eye.on.the.money.model.etf.ETFInvestment;
import eye.on.the.money.model.forex.ForexTransaction;
import eye.on.the.money.model.security.SecurityTransaction;
import eye.on.the.money.model.stock.Investment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityToStringTest {

    private static final String EMAIL = "owner@example.com";

    private static final User USER = User.builder().id(1L).email(EMAIL).password("hashed").build();

    @Test
    void investmentDoesNotLeakTheOwner() {
        assertFalse(Investment.builder().user(USER).build().toString().contains(EMAIL));
    }

    @Test
    void forexTransactionDoesNotLeakTheOwner() {
        assertFalse(ForexTransaction.builder().user(USER).build().toString().contains(EMAIL));
    }

    @Test
    void securityTransactionDoesNotLeakTheOwner() {
        assertFalse(SecurityTransaction.builder().user(USER).build().toString().contains(EMAIL));
    }

    @Test
    void etfInvestmentDoesNotLeakTheOwner() {
        assertFalse(ETFInvestment.builder().user(USER).build().toString().contains(EMAIL));
    }

    @Test
    void credentialDoesNotLeakItsSecret() {
        Credential credential = new Credential("eod.api.key", "super-secret-value");

        assertFalse(credential.toString().contains("super-secret-value"));
        assertTrue(credential.toString().contains("eod.api.key"));
    }

    @Test
    void userDoesNotLeakItsPassword() {
        assertFalse(USER.toString().contains("hashed"));
    }
}
