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
                .quantity(667)
                .buySell("B")
                .shortName("AMD")
                .build();
        InvestmentDTO baseDTO = this.getBaseDTO();

        iDTO1.mergeInvestments(iDTO2);

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
                .quantity(6)
                .buySell("B")
                .shortName("AMD")
                .build();
        InvestmentDTO baseDTO = this.getBaseDTO();

        iDTO1.mergeInvestments(iDTO2);

        Assertions.assertAll("Assert all changing values",
                () -> assertEquals(iDTO2.getAmount() + baseDTO.getAmount(), iDTO1.getAmount()),
                () -> assertEquals(iDTO2.getQuantity() + baseDTO.getQuantity(), iDTO1.getQuantity()),
                () -> assertEquals("B", iDTO1.getBuySell()));
    }

    @Test
    public void mergeInvestmentsDifferentShortName() {
        InvestmentDTO iDTO1 = this.getBaseDTO();
        InvestmentDTO iDTO2 = InvestmentDTO.builder()
                .amount(15.0)
                .quantity(667)
                .buySell("B")
                .shortName("CRSR")
                .build();

        iDTO1.mergeInvestments(iDTO2);

        Assertions.assertAll("Assert all changing values",
                () -> assertEquals(15.0, iDTO1.getAmount()),
                () -> assertEquals(667, iDTO1.getQuantity()),
                () -> assertEquals("B", iDTO1.getBuySell()));
    }

    @Test
    public void negateAmountAndQuantity() {
        InvestmentDTO iDTO = InvestmentDTO.builder().amount(15.0).quantity(667).build();
        iDTO.negateAmountAndQuantity();

        Assertions.assertAll("Assert all negated values",
                () -> assertEquals(-15.0, iDTO.getAmount()),
                () -> assertEquals(-667, iDTO.getQuantity()));
    }

    @Test
    public void negateAmountAndZero() {
        InvestmentDTO iDTO = InvestmentDTO.builder().amount(0.0).quantity(0).build();
        iDTO.negateAmountAndQuantity();

        Assertions.assertAll("Assert all negated values",
                () -> assertEquals(-0.0, iDTO.getAmount()),
                () -> assertEquals(0, iDTO.getQuantity()));
    }

    @Test
    public void negateAmountAndMinus() {
        InvestmentDTO iDTO = InvestmentDTO.builder().amount(-78.1).quantity(-6123).build();
        iDTO.negateAmountAndQuantity();

        Assertions.assertAll("Assert all negated values",
                () -> assertEquals(78.1, iDTO.getAmount()),
                () -> assertEquals(6123, iDTO.getQuantity()));
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
                () -> assertEquals(667, iDTO.getCSVRecord()[1]),
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
                () -> assertEquals(667, iDTO.getQuantity()),
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

    private InvestmentDTO getBaseDTO() {
        return InvestmentDTO.builder()
                .investmentId(1L)
                .amount(15.0)
                .quantity(667)
                .buySell("B")
                .exchange("NYSE")
                .shortName("AMD")
                .currencyId("USD")
                .fee(0.0)
                .accountName("Account")
                .build();
    }
}