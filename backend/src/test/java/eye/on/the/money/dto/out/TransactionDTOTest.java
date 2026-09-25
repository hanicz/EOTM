package eye.on.the.money.dto.out;

import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class TransactionDTOTest {

    @Mock
    private CSVRecord record;

    @Test
    public void mergeInvestments() {
        TransactionDTO tDTO1 = this.getBaseDTO();
        TransactionDTO tDTO2 = TransactionDTO.builder()
                .amount(new BigDecimal("15"))
                .quantity(new BigDecimal("667.4"))
                .buySell("B")
                .symbol("BTC")
                .build();
        TransactionDTO baseDTO = this.getBaseDTO();

        tDTO1.merge(tDTO2);

        Assertions.assertAll("Assert all changing values",
                () -> assertDecimal(tDTO2.getAmount().add(baseDTO.getAmount()), tDTO1.getAmount()),
                () -> assertDecimal(tDTO2.getQuantity().add(baseDTO.getQuantity()), tDTO1.getQuantity()),
                () -> assertEquals("B", tDTO1.getBuySell()));
    }

    @Test
    public void mergeInvestmentsWithSell() {
        TransactionDTO tDTO1 = this.getBaseDTO();
        tDTO1.setBuySell("S");
        TransactionDTO tDTO2 = TransactionDTO.builder()
                .amount(new BigDecimal("10"))
                .quantity(new BigDecimal("6"))
                .buySell("B")
                .symbol("BTC")
                .build();
        TransactionDTO baseDTO = this.getBaseDTO();

        tDTO1.merge(tDTO2);

        Assertions.assertAll("Assert all changing values",
                () -> assertDecimal(tDTO2.getAmount().add(baseDTO.getAmount()), tDTO1.getAmount()),
                () -> assertDecimal(tDTO2.getQuantity().add(baseDTO.getQuantity()), tDTO1.getQuantity()),
                () -> assertEquals("B", tDTO1.getBuySell()));
    }

    @Test
    public void mergeClosesAPositionExactly() {
        TransactionDTO tDTO1 = TransactionDTO.builder()
                .amount(new BigDecimal("300")).quantity(new BigDecimal("0.3")).buySell("B").symbol("BTC").build();
        TransactionDTO tDTO2 = TransactionDTO.builder()
                .amount(new BigDecimal("-350")).quantity(new BigDecimal("-0.1")).buySell("B").symbol("BTC").build();
        TransactionDTO tDTO3 = TransactionDTO.builder()
                .amount(new BigDecimal("-100")).quantity(new BigDecimal("-0.2")).buySell("B").symbol("BTC").build();

        tDTO1.merge(tDTO2).merge(tDTO3);

        Assertions.assertAll("0.3 - 0.1 - 0.2 is exactly zero and counts as closed",
                () -> assertDecimal("0", tDTO1.getQuantity()),
                () -> Assertions.assertTrue(tDTO1.isClosed()),
                () -> assertDecimal("-150", tDTO1.getAmount()));
    }

    @Test
    public void mergeInvestmentsDifferentSymbol() {
        TransactionDTO tDTO1 = this.getBaseDTO();
        TransactionDTO tDTO2 = TransactionDTO.builder()
                .amount(new BigDecimal("15"))
                .quantity(new BigDecimal("667.4"))
                .buySell("B")
                .symbol("ETH")
                .build();

        tDTO1.merge(tDTO2);

        Assertions.assertAll("Assert all changing values",
                () -> assertDecimal("15", tDTO1.getAmount()),
                () -> assertDecimal("667.4", tDTO1.getQuantity()),
                () -> assertEquals("B", tDTO1.getBuySell()));
    }

    @Test
    public void negateAmountAndQuantity() {
        TransactionDTO tDTO = TransactionDTO.builder().amount(new BigDecimal("15")).quantity(new BigDecimal("667.2")).build();
        tDTO.negateAmountAndQuantity();

        Assertions.assertAll("Assert all negated values",
                () -> assertDecimal("-15", tDTO.getAmount()),
                () -> assertDecimal("-667.2", tDTO.getQuantity()));
    }

    @Test
    public void negateAmountAndZero() {
        TransactionDTO tDTO = TransactionDTO.builder().amount(BigDecimal.ZERO).quantity(BigDecimal.ZERO).build();
        tDTO.negateAmountAndQuantity();

        Assertions.assertAll("Assert all negated values",
                () -> assertDecimal("0", tDTO.getAmount()),
                () -> assertDecimal("0", tDTO.getQuantity()));
    }

    @Test
    public void negateAmountAndMinus() {
        TransactionDTO tDTO = TransactionDTO.builder().amount(new BigDecimal("-78.1")).quantity(new BigDecimal("-6123.3")).build();
        tDTO.negateAmountAndQuantity();

        Assertions.assertAll("Assert all negated values",
                () -> assertDecimal("78.1", tDTO.getAmount()),
                () -> assertDecimal("6123.3", tDTO.getQuantity()));
    }

    @Test
    public void getHeaders() {
        TransactionDTO tDTO = TransactionDTO.builder().build();

        Assertions.assertAll("Assert all headers",
                () -> assertEquals("Transaction Id", tDTO.getHeaders()[0]),
                () -> assertEquals("Quantity", tDTO.getHeaders()[1]),
                () -> assertEquals("Type", tDTO.getHeaders()[2]),
                () -> assertEquals("Transaction Date", tDTO.getHeaders()[3]),
                () -> assertEquals("Symbol", tDTO.getHeaders()[4]),
                () -> assertEquals("Amount", tDTO.getHeaders()[5]),
                () -> assertEquals("Currency", tDTO.getHeaders()[6]),
                () -> assertEquals("Fee", tDTO.getHeaders()[7])
        );
    }

    @Test
    public void getCSVRecord() {
        LocalDate ld = LocalDate.now();
        TransactionDTO tDTO = this.getBaseDTO();
        tDTO.setTransactionDate(ld);

        Assertions.assertAll("Assert all headers",
                () -> assertEquals(1L, tDTO.getCSVRecord()[0]),
                () -> assertEquals("667.4", tDTO.getCSVRecord()[1]),
                () -> assertEquals("B", tDTO.getCSVRecord()[2]),
                () -> assertEquals(ld, tDTO.getCSVRecord()[3]),
                () -> assertEquals("BTC", tDTO.getCSVRecord()[4]),
                () -> assertEquals("15", tDTO.getCSVRecord()[5]),
                () -> assertEquals("USD", tDTO.getCSVRecord()[6]),
                () -> assertEquals("3.4", tDTO.getCSVRecord()[7])
        );
    }

    @Test
    public void createFromCSVRecord() {
        when(this.record.get("Transaction Date")).thenReturn("2020-01-01");
        when(this.record.get("Transaction Id")).thenReturn("1");
        when(this.record.get("Type")).thenReturn("B");
        when(this.record.get("Quantity")).thenReturn("667.4");
        when(this.record.get("Symbol")).thenReturn("BTC");
        when(this.record.get("Amount")).thenReturn("15.0");
        when(this.record.get("Currency")).thenReturn("USD");
        when(this.record.get("Fee")).thenReturn("3.4");

        TransactionDTO tDTO = TransactionDTO.createFromCSVRecord(this.record, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertAll("Assert all values",
                () -> assertEquals(1L, tDTO.getId()),
                () -> assertDecimal("667.4", tDTO.getQuantity()),
                () -> assertEquals("B", tDTO.getBuySell()),
                () -> assertEquals(LocalDate.parse("2020-01-01"), tDTO.getTransactionDate()),
                () -> assertEquals("BTC", tDTO.getSymbol()),
                () -> assertDecimal("15", tDTO.getAmount()),
                () -> assertEquals("USD", tDTO.getCurrencyId()),
                () -> assertDecimal("3.4", tDTO.getFee())
        );
    }

    private TransactionDTO getBaseDTO() {
        return TransactionDTO.builder()
                .id(1L)
                .amount(new BigDecimal("15"))
                .quantity(new BigDecimal("667.4"))
                .buySell("B")
                .symbol("BTC")
                .currencyId("USD")
                .fee(new BigDecimal("3.4"))
                .build();
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        assertDecimal(new BigDecimal(expected), actual);
    }

    private static void assertDecimal(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual), () -> "expected " + expected + " but was " + actual);
    }
}
