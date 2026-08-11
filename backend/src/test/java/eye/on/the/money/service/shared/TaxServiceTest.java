package eye.on.the.money.service.shared;

import eye.on.the.money.dto.in.EODCandleQuoteDTO;
import eye.on.the.money.dto.in.RSUDTO;
import eye.on.the.money.dto.out.RSUTaxDTO;
import eye.on.the.money.dto.out.TaxBreakdownDTO;
import eye.on.the.money.dto.out.TaxReportDTO;
import eye.on.the.money.exception.TaxException;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.service.api.EODAPIService;
import eye.on.the.money.service.api.MNBAPIService;
import eye.on.the.money.service.stock.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaxServiceTest {

    private static final String SHORT_NAME = "AAPL";
    private static final LocalDate VEST_DATE = LocalDate.of(2024, 6, 3);

    @Mock
    private EODAPIService eodAPIService;
    @Mock
    private MNBAPIService mnbAPIService;
    @Mock
    private StockService stockService;

    private TaxService taxService;

    @BeforeEach
    void setUp() {
        this.taxService = new TaxService(this.eodAPIService, this.mnbAPIService, this.stockService,
                new TaxCalculator());
        when(this.stockService.getAllExchanges()).thenReturn(
                List.of(Exchange.builder().code("US").name("USA Stocks").currency("USD").build()));
    }

    private RSUDTO rsu(int quantity, LocalDate date) {
        return RSUDTO.builder().shortName(SHORT_NAME).date(date).quantity(quantity).build();
    }

    private void stubClose(LocalDate date, double close) {
        when(this.eodAPIService.getHistoricalQuotes(anyString(), any(), any())).thenReturn(
                List.of(EODCandleQuoteDTO.builder().date(date).close(close).build()));
    }

    private void stubRates(Map<LocalDate, String> usdRates) {
        NavigableMap<LocalDate, BigDecimal> rates = new TreeMap<>();
        usdRates.forEach((date, rate) -> rates.put(date, new BigDecimal(rate)));
        when(this.mnbAPIService.getExchangeRates(any(), any(), any())).thenReturn(Map.of("USD", rates));
    }

    @Test
    void calculateTax_chargesBothTaxesOn89PercentOfTheAmount() {
        TaxBreakdownDTO tax = this.taxService.calculateTax(new BigDecimal("100000"));

        assertEquals(0, new BigDecimal("89000").compareTo(tax.getTaxBase()));
        assertEquals(0, new BigDecimal("11570").compareTo(tax.getSzocho()));
        assertEquals(0, new BigDecimal("13350").compareTo(tax.getSzja()));
        assertEquals(0, new BigDecimal("24920").compareTo(tax.getTotal()));
    }

    @Test
    void calculateTax_taxesALossAtZero() {
        TaxBreakdownDTO tax = this.taxService.calculateTax(new BigDecimal("-100000"));

        assertEquals(0, BigDecimal.ZERO.compareTo(tax.getTotal()));
    }

    @Test
    void calculateTaxForRSUs_valuesTheSharesAtTheCloseConvertedToHuf() {
        this.stubClose(VEST_DATE, 150.0);
        this.stubRates(Map.of(VEST_DATE, "350"));

        RSUTaxDTO item = this.taxService.calculateTaxForRSUs(List.of(this.rsu(10, VEST_DATE))).getItems().getFirst();

        // 10 * 150 USD = 1500 USD at 350 HUF.
        assertEquals(0, new BigDecimal("1500.00").compareTo(item.getAmount()));
        assertEquals(0, new BigDecimal("525000.00").compareTo(item.getAmountInHuf()));
        // 89% of 525 000 is 467 250, taxed at 13% and 15%.
        assertEquals(0, new BigDecimal("467250.00").compareTo(item.getTax().getTaxBase()));
        assertEquals(0, new BigDecimal("60743").compareTo(item.getTax().getSzocho()));
        assertEquals(0, new BigDecimal("70088").compareTo(item.getTax().getSzja()));
        assertEquals(0, new BigDecimal("130831").compareTo(item.getTax().getTotal()));
    }

    @Test
    void calculateTaxForRSUs_takesCurrencyFromTheExchange() {
        this.stubClose(VEST_DATE, 150.0);
        this.stubRates(Map.of(VEST_DATE, "350"));

        RSUTaxDTO item = this.taxService.calculateTaxForRSUs(List.of(this.rsu(10, VEST_DATE))).getItems().getFirst();

        assertEquals("USD", item.getCurrency());
        assertEquals("US", item.getExchange());
    }

    @Test
    void calculateTaxForRSUs_prefersTheCurrencyGivenOnTheRequest() {
        this.stubClose(VEST_DATE, 150.0);
        NavigableMap<LocalDate, BigDecimal> eur = new TreeMap<>();
        eur.put(VEST_DATE, new BigDecimal("395"));
        when(this.mnbAPIService.getExchangeRates(any(), any(), any())).thenReturn(Map.of("EUR", eur));

        RSUTaxDTO item = this.taxService.calculateTaxForRSUs(List.of(
                RSUDTO.builder().shortName(SHORT_NAME).date(VEST_DATE).quantity(10).currency("EUR").build()
        )).getItems().getFirst();

        assertEquals("EUR", item.getCurrency());
        assertEquals(0, new BigDecimal("592500.00").compareTo(item.getAmountInHuf()));
        verify(this.stockService, never()).getAllExchanges();
    }

    @Test
    void calculateTaxForRSUs_buildsTheTickerFromShortNameAndExchange() {
        this.stubClose(VEST_DATE, 150.0);
        this.stubRates(Map.of(VEST_DATE, "350"));

        this.taxService.calculateTaxForRSUs(List.of(
                RSUDTO.builder().shortName("vwrl").exchange("lse").date(VEST_DATE).quantity(1).currency("USD").build()));

        verify(this.eodAPIService).getHistoricalQuotes(eq("VWRL.LSE"), any(), any());
    }

    @Test
    void calculateTaxForRSUs_fallsBackToTheLastCloseAndRateBeforeANonTradingDay() {
        LocalDate easterMonday = LocalDate.of(2024, 4, 1);
        LocalDate maundyThursday = LocalDate.of(2024, 3, 28);
        this.stubClose(maundyThursday, 150.0);
        this.stubRates(Map.of(maundyThursday, "300"));

        RSUTaxDTO item = this.taxService.calculateTaxForRSUs(List.of(this.rsu(10, easterMonday)))
                .getItems().getFirst();

        assertEquals(easterMonday, item.getDate());
        assertEquals(maundyThursday, item.getPriceDate());
        assertEquals(maundyThursday, item.getRateDate());
    }

    @Test
    void calculateTaxForRSUs_totalsAcrossRows() {
        this.stubClose(VEST_DATE, 150.0);
        this.stubRates(Map.of(VEST_DATE, "350"));

        TaxReportDTO report = this.taxService.calculateTaxForRSUs(
                List.of(this.rsu(10, VEST_DATE), this.rsu(5, VEST_DATE)));

        assertEquals(2, report.getItems().size());
        assertEquals(0, new BigDecimal("787500.00").compareTo(report.getTotalAmountInHuf()));

        BigDecimal summed = report.getItems().stream().map(item -> item.getTax().getTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, summed.compareTo(report.getTotalTax().getTotal()));
    }

    @Test
    void calculateTaxForRSUs_asksTheExchangeListOnlyOncePerTicker() {
        this.stubClose(VEST_DATE, 150.0);
        this.stubRates(Map.of(VEST_DATE, "350"));

        this.taxService.calculateTaxForRSUs(List.of(this.rsu(10, VEST_DATE), this.rsu(5, VEST_DATE)));

        verify(this.stockService).getAllExchanges();
    }

    @Test
    void calculateTaxForRSUs_failsWhenNoCloseExistsOnOrBeforeTheDate() {
        // The only quote is after the vest date, so there is nothing to value it at.
        this.stubClose(LocalDate.of(2024, 7, 1), 150.0);
        this.stubRates(Map.of(VEST_DATE, "350"));

        List<RSUDTO> rsus = List.of(this.rsu(10, VEST_DATE));

        assertThrows(TaxException.class, () -> this.taxService.calculateTaxForRSUs(rsus));
    }

    @Test
    void calculateTaxForRSUs_failsWhenNoRateExistsOnOrBeforeTheDate() {
        this.stubClose(VEST_DATE, 150.0);
        this.stubRates(Map.of(LocalDate.of(2024, 7, 1), "350"));

        List<RSUDTO> rsus = List.of(this.rsu(10, VEST_DATE));

        assertThrows(TaxException.class, () -> this.taxService.calculateTaxForRSUs(rsus));
    }

    @Test
    void calculateTaxForRSUs_failsWhenTheExchangeCurrencyIsUnknown() {
        when(this.stockService.getAllExchanges()).thenReturn(List.of());

        List<RSUDTO> rsus = List.of(this.rsu(10, VEST_DATE));

        assertThrows(TaxException.class, () -> this.taxService.calculateTaxForRSUs(rsus));
    }

    @Test
    void calculateTaxForRSUs_returnsEmptyReportForNoRows() {
        TaxReportDTO report = this.taxService.calculateTaxForRSUs(List.of());

        assertEquals(0, report.getItems().size());
        assertEquals(0, BigDecimal.ZERO.compareTo(report.getTotalTax().getTotal()));
    }
}
