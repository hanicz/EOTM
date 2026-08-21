package eye.on.the.money.service.shared;

import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.AssetClassValueDTO;
import eye.on.the.money.dto.out.DashboardRatesDTO;
import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.dto.out.ForexTransactionDTO;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.dto.out.NetWorthDTO;
import eye.on.the.money.dto.out.SecurityTransactionDTO;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.service.crypto.TransactionService;
import eye.on.the.money.service.etf.ETFInvestmentService;
import eye.on.the.money.service.forex.ForexTransactionService;
import eye.on.the.money.service.security.SecurityTransactionService;
import eye.on.the.money.service.stock.InvestmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NetWorthServiceTest {

    private static final String USER = "user@eotm.com";

    @Mock
    private InvestmentService investmentService;
    @Mock
    private TransactionService transactionService;
    @Mock
    private ETFInvestmentService etfInvestmentService;
    @Mock
    private ForexTransactionService forexTransactionService;
    @Mock
    private SecurityTransactionService securityTransactionService;
    @Mock
    private DashboardService dashboardService;

    private NetWorthService netWorthService;

    @BeforeEach
    void setUp() {
        this.netWorthService = new NetWorthService(this.investmentService, this.transactionService,
                this.etfInvestmentService, this.forexTransactionService, this.securityTransactionService,
                this.dashboardService);

        this.stubRates(Map.of("USD", 1.10, "HUF", 400.0));
        when(this.investmentService.getCurrentHoldings(anyString())).thenReturn(List.of());
        when(this.transactionService.getCurrentHoldings(anyString(), any())).thenReturn(List.of());
        when(this.etfInvestmentService.getCurrentETFHoldings(anyString())).thenReturn(List.of());
        when(this.forexTransactionService.getAllForexHoldings(anyString())).thenReturn(List.of());
        when(this.securityTransactionService.getCurrentHoldings(anyString())).thenReturn(List.of());
    }

    private void stubRates(Map<String, Double> rates) {
        when(this.dashboardService.getConversionRates(any()))
                .thenReturn(DashboardRatesDTO.builder().rates(rates).build());
    }

    private AssetClassValueDTO assetOf(NetWorthDTO netWorth, String assetClass) {
        return netWorth.getAssets().stream()
                .filter(asset -> assetClass.equals(asset.getAssetClass())).findFirst().orElseThrow();
    }

    @Test
    void getNetWorth_convertsStockLiveValueIntoTargetCurrency() {
        when(this.investmentService.getCurrentHoldings(USER)).thenReturn(List.of(
                InvestmentDTO.builder().amount(100.0).liveValue(150.0).currencyId("USD").build()));

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR");

        // 150 USD / 1.10 = 136.36 EUR
        assertEquals(0, this.assetOf(result, NetWorthService.STOCK).getWorth().compareTo(
                new BigDecimal("136.36")));
        assertEquals(0, result.getTotalWorth().compareTo(new BigDecimal("136.36")));
    }

    /**
     * The bug this service was written to fix: crypto quotes its live value in the currency the holdings were
     * requested in, which is always EUR, while the cost basis stays in the currency actually paid.
     */
    @Test
    void getNetWorth_treatsCryptoLiveValueAsEurNotAsThePurchaseCurrency() {
        when(this.transactionService.getCurrentHoldings(anyString(), any())).thenReturn(List.of(
                TransactionDTO.builder().amount(44000.0).liveValue(200.0).currencyId("HUF").build()));

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR");

        // The live value is already EUR, so it stays 200 rather than being divided by the HUF rate.
        assertEquals(0, this.assetOf(result, NetWorthService.CRYPTO).getWorth().compareTo(
                new BigDecimal("200.00")));
        // The cost basis is genuinely HUF: 44000 / 400 = 110 EUR.
        assertEquals(0, this.assetOf(result, NetWorthService.CRYPTO).getSpent().compareTo(
                new BigDecimal("110.00")));
    }

    @Test
    void getNetWorth_requestsCryptoHoldingsInBaseCurrency() {
        this.netWorthService.getNetWorth(USER, "HUF");

        ArgumentCaptor<TransactionQuery> captor = ArgumentCaptor.forClass(TransactionQuery.class);
        verify(this.transactionService).getCurrentHoldings(eq(USER), captor.capture());

        assertEquals("EUR", captor.getValue().getCurrency());
    }

    @Test
    void getNetWorth_fallsBackToCostWhenNoLivePriceIsAvailable() {
        when(this.etfInvestmentService.getCurrentETFHoldings(USER)).thenReturn(List.of(
                ETFInvestmentDTO.builder().amount(500.0).liveValue(null).currencyId("EUR").build()));

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR");

        assertEquals(0, this.assetOf(result, NetWorthService.ETF).getWorth().compareTo(
                new BigDecimal("500.00")));
    }

    @Test
    void getNetWorth_valuesForexHoldingsInTheCurrencySoldFrom() {
        when(this.forexTransactionService.getAllForexHoldings(USER)).thenReturn(List.of(
                ForexTransactionDTO.builder().fromAmount(1000.0).liveValue(1100.0)
                        .fromCurrencyId("EUR").toCurrencyId("USD").build()));

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR");

        assertEquals(0, this.assetOf(result, NetWorthService.FOREX).getWorth().compareTo(
                new BigDecimal("1100.00")));
    }

    /**
     * Interest is paid out and then reinvested by buying more, and those purchases already show up as
     * transactions. Counting the interest as well would count the same money twice.
     */
    @Test
    void getNetWorth_leavesInterestOutOfWhatSecuritiesAreWorth() {
        when(this.securityTransactionService.getCurrentHoldings(USER)).thenReturn(List.of(
                SecurityTransactionDTO.builder().amount(1000.0).currencyId("EUR").build()));

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR");

        AssetClassValueDTO securities = this.assetOf(result, NetWorthService.SECURITIES);
        assertEquals(0, securities.getSpent().compareTo(new BigDecimal("1000.00")));
        assertEquals(0, securities.getWorth().compareTo(new BigDecimal("1000.00")));
        assertEquals(0, securities.getChangePct().signum());
    }

    @Test
    void getNetWorth_reportsCurrenciesItCouldNotConvertRatherThanCountingThemAsZero() {
        this.stubRates(Map.of("USD", 1.10));
        when(this.investmentService.getCurrentHoldings(USER)).thenReturn(List.of(
                InvestmentDTO.builder().amount(100.0).liveValue(100.0).currencyId("GBP").build()));

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR");

        assertEquals(List.of("GBP"), result.getUnconvertedCurrencies());
        assertEquals(0, result.getTotalWorth().signum());
    }

    @Test
    void getNetWorth_listsEveryCurrencyHeldSoTheUiCanOfferThem() {
        when(this.investmentService.getCurrentHoldings(USER)).thenReturn(List.of(
                InvestmentDTO.builder().amount(10.0).liveValue(10.0).currencyId("USD").build()));

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "HUF");

        assertTrue(result.getAvailableCurrencies().containsAll(List.of("EUR", "HUF", "USD")));
    }

    @Test
    void getNetWorth_defaultsToBaseCurrencyWhenNoneIsGiven() {
        NetWorthDTO result = this.netWorthService.getNetWorth(USER, null);

        assertEquals("EUR", result.getCurrency());
    }

    @Test
    void getNetWorth_throwsWhenTheTargetCurrencyHasNoRate() {
        this.stubRates(Map.of("USD", 1.10));

        assertThrows(APIException.class, () -> this.netWorthService.getNetWorth(USER, "JPY"));
    }

    @Test
    void getNetWorth_reportsZeroChangeWhenNothingWasSpent() {
        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR");

        assertEquals(0, result.getTotalSpent().signum());
        assertEquals(0, result.getTotalChangePct().signum());
    }
}
