package eye.on.the.money.dto.out;

import eye.on.the.money.util.Lots;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ForexTransactionDTOTest {

    @Mock
    private CSVRecord record;

    @Test
    public void merge() {
        ForexTransactionDTO dto1 = this.getBaseTrans();
        ForexTransactionDTO dto2 = ForexTransactionDTO.builder()
                .fromAmount(20.3).toAmount(70.9).fromCurrencyId("USD").toCurrencyId("EUR").build();

        ForexTransactionDTO baseDTO = this.getBaseTrans();

        dto1.merge(dto2);

        Assertions.assertAll("Assert all changing values",
                () -> assertEquals(baseDTO.getFromAmount() + dto2.getFromAmount(), dto1.getFromAmount()),
                () -> assertEquals(baseDTO.getToAmount() + dto2.getToAmount(), dto1.getToAmount()),
                () -> assertEquals(dto1.getFromAmount() / dto1.getToAmount(), dto1.getChangeRate()));
    }

    @Test
    public void mergeIgnoresADifferentPair() {
        ForexTransactionDTO dto1 = this.getBaseTrans();
        ForexTransactionDTO dto2 = ForexTransactionDTO.builder()
                .fromAmount(20.3).toAmount(70.9).fromCurrencyId("HUF").toCurrencyId("EUR").build();

        dto1.merge(dto2);

        Assertions.assertAll("An unrelated pair must not touch the lot",
                () -> assertEquals(10.5, dto1.getFromAmount()),
                () -> assertEquals(50.2, dto1.getToAmount()),
                () -> assertEquals(0.5, dto1.getChangeRate()));
    }

    @Test
    public void mergeKeepsTheRateOfASealedLot() {
        ForexTransactionDTO dto1 = this.pair("HUF", 400000.0, "EUR", 1000.0, "B", "2024-01-01", 400.0);
        ForexTransactionDTO dto2 = this.pair("HUF", -420000.0, "EUR", -1000.0, "S", "2024-02-01", 420.0);

        dto1.merge(dto2);

        Assertions.assertAll("A closed lot must not divide by zero",
                () -> assertEquals(0.0, dto1.getToAmount()),
                () -> assertEquals(-20000.0, dto1.getFromAmount()),
                () -> assertEquals(400.0, dto1.getChangeRate()),
                () -> Assertions.assertTrue(dto1.isClosed()));
    }

    @Test
    public void mergeFlipsASoldLotBackToBuyWhenItTurnsPositive() {
        ForexTransactionDTO dto1 = this.pair("HUF", -160000.0, "EUR", -400.0, "S", "2024-02-01", 400.0);
        ForexTransactionDTO dto2 = this.pair("HUF", 400000.0, "EUR", 1000.0, "B", "2024-01-01", 400.0);

        dto1.merge(dto2);

        assertEquals("B", dto1.getBuySell());
    }

    @Test
    public void aReversedSellReducesTheMatchingBuy() {
        Map<String, ForexTransactionDTO> lots = Lots.aggregate(List.of(
                this.pair("HUF", 400000.0, "EUR", 1000.0, "B", "2024-01-01", 400.0),
                this.pair("EUR", 400.0, "HUF", 160000.0, "S", "2024-02-01", 400.0)), this.pairKey());

        ForexTransactionDTO lot = lots.get("HUFEUR_0");

        Assertions.assertAll("The sell must unwind part of the buy",
                () -> assertEquals(1, lots.size()),
                () -> assertEquals(240000.0, lot.getFromAmount()),
                () -> assertEquals(600.0, lot.getToAmount()),
                () -> assertEquals(400.0, lot.getChangeRate()),
                () -> assertEquals("B", lot.getBuySell()));
    }

    @Test
    public void aFullyUnwoundPairIsSealedWithItsRealisedGain() {
        Map<String, ForexTransactionDTO> lots = Lots.aggregate(List.of(
                this.pair("HUF", 400000.0, "EUR", 1000.0, "B", "2024-01-01", 400.0),
                this.pair("EUR", 1000.0, "HUF", 420000.0, "S", "2024-02-01", 420.0)), this.pairKey());

        ForexTransactionDTO lot = lots.get("HUFEUR_0");

        Assertions.assertAll("The realised gain stays on the closed lot",
                () -> assertEquals(1, lots.size()),
                () -> assertEquals(0.0, lot.getToAmount()),
                () -> assertEquals(-20000.0, lot.getFromAmount()),
                () -> Assertions.assertTrue(lot.isClosed()));
    }

    @Test
    public void aRebuyAfterAFullUnwindStartsAFreshCostBasis() {
        Map<String, ForexTransactionDTO> lots = Lots.aggregate(List.of(
                this.pair("HUF", 400000.0, "EUR", 1000.0, "B", "2024-01-01", 400.0),
                this.pair("EUR", 1000.0, "HUF", 420000.0, "S", "2024-02-01", 420.0),
                this.pair("HUF", 210000.0, "EUR", 500.0, "B", "2024-03-01", 420.0)), this.pairKey());

        Assertions.assertAll("The re-buy must not inherit the closed lot",
                () -> assertEquals(2, lots.size()),
                () -> assertEquals(0.0, lots.get("HUFEUR_0").getToAmount()),
                () -> assertEquals(210000.0, lots.get("HUFEUR_1").getFromAmount()),
                () -> assertEquals(500.0, lots.get("HUFEUR_1").getToAmount()));
    }

    @Test
    public void aClosedPairDoesNotBlockAnUnrelatedPair() {
        Map<String, ForexTransactionDTO> lots = Lots.aggregate(List.of(
                this.pair("HUF", 400000.0, "EUR", 1000.0, "B", "2024-01-01", 400.0),
                this.pair("HUF", 380000.0, "USD", 1000.0, "B", "2024-01-02", 380.0),
                this.pair("EUR", 1000.0, "HUF", 420000.0, "S", "2024-02-01", 420.0)), this.pairKey());

        Assertions.assertAll("Closing HUFEUR must not touch HUFUSD",
                () -> assertEquals(2, lots.size()),
                () -> assertEquals(0.0, lots.get("HUFEUR_0").getToAmount()),
                () -> assertEquals(1000.0, lots.get("HUFUSD_0").getToAmount()));
    }

    @Test
    public void getHeaders() {
        ForexTransactionDTO dto = ForexTransactionDTO.builder().build();

        Assertions.assertAll("Assert all headers",
                () -> assertEquals("Transaction Id", dto.getHeaders()[0]),
                () -> assertEquals("From Amount", dto.getHeaders()[1]),
                () -> assertEquals("To Amount", dto.getHeaders()[2]),
                () -> assertEquals("Type", dto.getHeaders()[3]),
                () -> assertEquals("Transaction Date", dto.getHeaders()[4]),
                () -> assertEquals("Change Rate", dto.getHeaders()[5]),
                () -> assertEquals("From Currency", dto.getHeaders()[6]),
                () -> assertEquals("To Currency", dto.getHeaders()[7])
        );
    }

    @Test
    public void getCSVRecord() {
        LocalDate ld = LocalDate.now();
        ForexTransactionDTO dto = this.getBaseTrans();
        dto.setTransactionDate(ld);

        Assertions.assertAll("Assert all headers",
                () -> assertEquals(1L, dto.getCSVRecord()[0]),
                () -> assertEquals(10.5, dto.getCSVRecord()[1]),
                () -> assertEquals(50.2, dto.getCSVRecord()[2]),
                () -> assertEquals("F", dto.getCSVRecord()[3]),
                () -> assertEquals(ld, dto.getCSVRecord()[4]),
                () -> assertEquals(0.5, dto.getCSVRecord()[5]),
                () -> assertEquals("USD", dto.getCSVRecord()[6]),
                () -> assertEquals("EUR", dto.getCSVRecord()[7])
        );
    }

    @Test
    public void createFromCSVRecord() {
        when(this.record.get("Transaction Date")).thenReturn("2020-01-01");
        when(this.record.get("Transaction Id")).thenReturn("1");
        when(this.record.get("Type")).thenReturn("F");
        when(this.record.get("From Amount")).thenReturn("10.5");
        when(this.record.get("To Amount")).thenReturn("50.2");
        when(this.record.get("Change Rate")).thenReturn("0.5");
        when(this.record.get("From Currency")).thenReturn("USD");
        when(this.record.get("To Currency")).thenReturn("EUR");

        ForexTransactionDTO tDTO = ForexTransactionDTO.createFromCSVRecord(this.record, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertAll("Assert all values",
                () -> assertEquals(1L, tDTO.getForexTransactionId()),
                () -> assertEquals(10.5, tDTO.getFromAmount()),
                () -> assertEquals(50.2, tDTO.getToAmount()),
                () -> assertEquals("F", tDTO.getBuySell()),
                () -> assertEquals(LocalDate.parse("2020-01-01"), tDTO.getTransactionDate()),
                () -> assertEquals(0.5, tDTO.getChangeRate()),
                () -> assertEquals("USD", tDTO.getFromCurrencyId()),
                () -> assertEquals("EUR", tDTO.getToCurrencyId())
        );
    }

    private Function<ForexTransactionDTO, String> pairKey() {
        return ft -> ft.getFromCurrencyId() + ft.getToCurrencyId();
    }

    private ForexTransactionDTO pair(String fromCurrencyId, Double fromAmount, String toCurrencyId, Double toAmount,
                                     String buySell, String date, Double changeRate) {
        return ForexTransactionDTO.builder()
                .fromCurrencyId(fromCurrencyId)
                .fromAmount(fromAmount)
                .toCurrencyId(toCurrencyId)
                .toAmount(toAmount)
                .buySell(buySell)
                .transactionDate(LocalDate.parse(date))
                .changeRate(changeRate)
                .build();
    }

    private ForexTransactionDTO getBaseTrans() {
        return ForexTransactionDTO.builder()
                .forexTransactionId(1L)
                .fromAmount(10.5)
                .toAmount(50.2)
                .buySell("F")
                .changeRate(0.5)
                .fromCurrencyId("USD")
                .toCurrencyId("EUR")
                .build();
    }
}
