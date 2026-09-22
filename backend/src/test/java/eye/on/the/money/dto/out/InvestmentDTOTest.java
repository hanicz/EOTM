package eye.on.the.money.dto.out;

import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class InvestmentDTOTest {

    @Mock
    private CSVRecord record;

    @Test
    public void mergeInvestments() {
        InvestmentDTO iDTO1 = this.getBaseDTO();
        InvestmentDTO iDTO2 = InvestmentDTO.builder()
                .amount(15.0)
                .quantity(667.0)
                .buySell("B")
                .shortName("AMD")
                .accountId(1L)
                .exchange("NYSE")
                .build();
        InvestmentDTO baseDTO = this.getBaseDTO();

        iDTO1.merge(iDTO2);

        Assertions.assertAll("Assert all changing values",
                () -> assertEquals(iDTO2.getAmount() + baseDTO.getAmount(), iDTO1.getAmount()),
                () -> assertEquals(iDTO2.getQuantity() + baseDTO.getQuantity(), iDTO1.getQuantity()),
                () -> assertEquals("B", iDTO1.getBuySell()));
    }

    @Test
    public void mergeInvestmentsWithSell() {
        InvestmentDTO iDTO1 = this.getBaseDTO();
        iDTO1.setBuySell("S");
        InvestmentDTO iDTO2 = InvestmentDTO.builder()
                .amount(10.0)
                .quantity(6.0)
                .buySell("B")
                .shortName("AMD")
                .accountId(1L)
                .exchange("NYSE")
                .build();
        InvestmentDTO baseDTO = this.getBaseDTO();

        iDTO1.merge(iDTO2);

        Assertions.assertAll("Assert all changing values",
                () -> assertEquals(iDTO2.getAmount() + baseDTO.getAmount(), iDTO1.getAmount()),
                () -> assertEquals(iDTO2.getQuantity() + baseDTO.getQuantity(), iDTO1.getQuantity()),
                () -> assertEquals("B", iDTO1.getBuySell()));
    }

    @Test
    public void mergeInvestmentsDifferentExchange() {
        InvestmentDTO iDTO1 = this.getBaseDTO();
        InvestmentDTO iDTO2 = InvestmentDTO.builder()
                .amount(15.0)
                .quantity(667.0)
                .buySell("B")
                .shortName("AMD")
                .accountId(1L)
                .exchange("XETRA")
                .build();

        iDTO1.merge(iDTO2);

        Assertions.assertAll("Assert nothing merged",
                () -> assertEquals(15.0, iDTO1.getAmount()),
                () -> assertEquals(667.0, iDTO1.getQuantity()),
                () -> assertEquals("NYSE", iDTO1.getExchange()));
    }

    @Test
    public void mergeInvestmentsDifferentShortName() {
        InvestmentDTO iDTO1 = this.getBaseDTO();
        InvestmentDTO iDTO2 = InvestmentDTO.builder()
                .amount(15.0)
                .quantity(667.0)
                .buySell("B")
                .shortName("CRSR")
                .build();

        iDTO1.merge(iDTO2);

        Assertions.assertAll("Assert all changing values",
                () -> assertEquals(15.0, iDTO1.getAmount()),
                () -> assertEquals(667.0, iDTO1.getQuantity()),
                () -> assertEquals("B", iDTO1.getBuySell()));
    }

    @Test
    public void negateAmountAndQuantity() {
        InvestmentDTO iDTO = InvestmentDTO.builder().amount(15.0).quantity(667.0).build();
        iDTO.negateAmountAndQuantity();

        Assertions.assertAll("Assert all negated values",
                () -> assertEquals(-15.0, iDTO.getAmount()),
                () -> assertEquals(-667.0, iDTO.getQuantity()));
    }

    @Test
    public void negateAmountAndZero() {
        InvestmentDTO iDTO = InvestmentDTO.builder().amount(0.0).quantity(0.0).build();
        iDTO.negateAmountAndQuantity();

        Assertions.assertAll("Assert all negated values",
                () -> assertEquals(-0.0, iDTO.getAmount()),
                () -> assertEquals(-0.0, iDTO.getQuantity()));
    }

    @Test
    public void negateAmountAndMinus() {
        InvestmentDTO iDTO = InvestmentDTO.builder().amount(-78.1).quantity(-6123.0).build();
        iDTO.negateAmountAndQuantity();

        Assertions.assertAll("Assert all negated values",
                () -> assertEquals(78.1, iDTO.getAmount()),
                () -> assertEquals(6123.0, iDTO.getQuantity()));
    }

    @Test
    public void getHeaders() {
        InvestmentDTO iDTO = InvestmentDTO.builder().build();

        Assertions.assertAll("Assert all headers",
                () -> assertEquals("Investment Id", iDTO.getHeaders()[0]),
                () -> assertEquals("Quantity", iDTO.getHeaders()[1]),
                () -> assertEquals("Type", iDTO.getHeaders()[2]),
                () -> assertEquals("Transaction Date", iDTO.getHeaders()[3]),
                () -> assertEquals("Short Name", iDTO.getHeaders()[4]),
                () -> assertEquals("Exchange", iDTO.getHeaders()[5]),
                () -> assertEquals("Amount", iDTO.getHeaders()[6]),
                () -> assertEquals("Currency", iDTO.getHeaders()[7]),
                () -> assertEquals("Fee", iDTO.getHeaders()[8]),
                () -> assertEquals("Account", iDTO.getHeaders()[9])
        );
    }

    @Test
    public void getCSVRecord() {
        LocalDate ld = LocalDate.now();
        InvestmentDTO iDTO = this.getBaseDTO();
        iDTO.setTransactionDate(ld);

        Assertions.assertAll("Assert all headers",
                () -> assertEquals(1L, iDTO.getCSVRecord()[0]),
                () -> assertEquals("667", iDTO.getCSVRecord()[1]),
                () -> assertEquals("B", iDTO.getCSVRecord()[2]),
                () -> assertEquals(ld, iDTO.getCSVRecord()[3]),
                () -> assertEquals("AMD", iDTO.getCSVRecord()[4]),
                () -> assertEquals("NYSE", iDTO.getCSVRecord()[5]),
                () -> assertEquals(15.0, iDTO.getCSVRecord()[6]),
                () -> assertEquals("USD", iDTO.getCSVRecord()[7]),
                () -> assertEquals(0.0, iDTO.getCSVRecord()[8]),
                () -> assertEquals("Account", iDTO.getCSVRecord()[9])
        );
    }

    @Test
    public void createFromCSVRecord() {
        when(this.record.get("Investment Id")).thenReturn("1");
        when(this.record.get("Quantity")).thenReturn("667");
        when(this.record.get("Type")).thenReturn("B");
        when(this.record.get("Transaction Date")).thenReturn("2020-01-01");
        when(this.record.get("Short Name")).thenReturn("AMD");
        when(this.record.get("Exchange")).thenReturn("NYSE");
        when(this.record.get("Amount")).thenReturn("15.0");
        when(this.record.get("Currency")).thenReturn("USD");
        when(this.record.get("Fee")).thenReturn("0.0");
        when(this.record.get("Account")).thenReturn("Account");

        InvestmentDTO iDTO = InvestmentDTO.createFromCSVRecord(this.record, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertAll("Assert all values",
                () -> assertEquals(1L, iDTO.getInvestmentId()),
                () -> assertEquals(667.0, iDTO.getQuantity()),
                () -> assertEquals("B", iDTO.getBuySell()),
                () -> assertEquals(LocalDate.parse("2020-01-01"), iDTO.getTransactionDate()),
                () -> assertEquals("AMD", iDTO.getShortName()),
                () -> assertEquals("NYSE", iDTO.getExchange()),
                () -> assertEquals(15.0, iDTO.getAmount()),
                () -> assertEquals("USD", iDTO.getCurrencyId()),
                () -> assertEquals(0.0, iDTO.getFee()),
                () -> assertEquals("Account", iDTO.getAccountName())
        );
    }

    @Test
    public void fractionalSellClosesTheLot() {
        InvestmentDTO lot = this.fractional("B", 0.1, 10.0);
        lot.merge(this.fractional("B", 0.2, 20.0));
        InvestmentDTO sell = this.fractional("S", 0.3, 35.0);
        sell.negateAmountAndQuantity();

        lot.merge(sell);

        Assertions.assertAll("Assert the lot is closed",
                () -> assertEquals(0.0, lot.getQuantity()),
                () -> assertTrue(lot.isClosed()));
    }

    @Test
    public void fractionalPartialSellKeepsTheRemainder() {
        InvestmentDTO lot = this.fractional("B", 1.5, 150.0);
        InvestmentDTO sell = this.fractional("S", 0.5, 60.0);
        sell.negateAmountAndQuantity();

        lot.merge(sell);

        Assertions.assertAll("Assert the remainder stays open",
                () -> assertEquals(1.0, lot.getQuantity()),
                () -> assertFalse(lot.isClosed()));
    }

    @Test
    public void createFromCSVRecordWithFractionalQuantity() {
        when(this.record.get("Investment Id")).thenReturn("");
        when(this.record.get("Quantity")).thenReturn("0.25");
        when(this.record.get("Type")).thenReturn("B");
        when(this.record.get("Transaction Date")).thenReturn("2020-01-01");
        when(this.record.get("Short Name")).thenReturn("AMD");
        when(this.record.get("Exchange")).thenReturn("NYSE");
        when(this.record.get("Amount")).thenReturn("25.0");
        when(this.record.get("Currency")).thenReturn("USD");
        when(this.record.get("Fee")).thenReturn("0.0");
        when(this.record.get("Account")).thenReturn("Account");

        InvestmentDTO iDTO = InvestmentDTO.createFromCSVRecord(this.record, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertEquals(0.25, iDTO.getQuantity());
    }

    private InvestmentDTO fractional(String buySell, double quantity, double amount) {
        return InvestmentDTO.builder()
                .quantity(quantity)
                .amount(amount)
                .buySell(buySell)
                .shortName("AMD")
                .exchange("NYSE")
                .accountId(1L)
                .build();
    }

    private InvestmentDTO getBaseDTO() {
        return InvestmentDTO.builder()
                .investmentId(1L)
                .amount(15.0)
                .quantity(667.0)
                .buySell("B")
                .exchange("NYSE")
                .shortName("AMD")
                .currencyId("USD")
                .fee(0.0)
                .accountId(1L)
                .accountName("Account")
                .build();
    }
}