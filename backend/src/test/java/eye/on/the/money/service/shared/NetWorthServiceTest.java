package eye.on.the.money.service.shared;

import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.AssetClassValueDTO;
import eye.on.the.money.dto.out.CashDTO;
import eye.on.the.money.dto.out.DashboardRatesDTO;
import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.dto.out.ForexTransactionDTO;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.dto.out.NetWorthDTO;
import eye.on.the.money.dto.out.SecurityTransactionDTO;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.service.cash.CashService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NetWorthServiceTest {

    private static final Long USER = 1L;

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
    private CashService cashService;
    @Mock
    private DashboardService dashboardService;

    private NetWorthService netWorthService;

    @BeforeEach
    void setUp() {
        this.netWorthService = new NetWorthService(this.investmentService, this.transactionService,
                this.etfInvestmentService, this.forexTransactionService, this.securityTransactionService,
                this.cashService, this.dashboardService);

        this.stubRates(Map.of("USD", 1.10, "HUF", 400.0));
        when(this.investmentService.getCurrentHoldings(anyLong())).thenReturn(List.of());
        when(this.transactionService.getCurrentHoldings(anyLong(), any())).thenReturn(List.of());
        when(this.etfInvestmentService.getCurrentETFHoldings(anyLong())).thenReturn(List.of());
        when(this.forexTransactionService.getAllForexHoldings(anyLong())).thenReturn(List.of());
        when(this.securityTransactionService.getCurrentHoldings(anyLong())).thenReturn(List.of());
        this.stubCash(0.0);
    }

    private void stubCash(Double amount) {
        when(this.cashService.getCash(anyLong()))
                .thenReturn(CashDTO.builder().amount(amount).currency("HUF").build());
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

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR", false);

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
        when(this.transactionService.getCurrentHoldings(anyLong(), any())).thenReturn(List.of(
                TransactionDTO.builder().amount(44000.0).liveValue(200.0).currencyId("HUF").build()));

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR", false);

        // The live value is already EUR, so it stays 200 rather than being divided by the HUF rate.
        assertEquals(0, this.assetOf(result, NetWorthService.CRYPTO).getWorth().compareTo(
                new BigDecimal("200.00")));
        // The cost basis is genuinely HUF: 44000 / 400 = 110 EUR.
        assertEquals(0, this.assetOf(result, NetWorthService.CRYPTO).getSpent().compareTo(
                new BigDecimal("110.00")));
    }

    @Test
    void getNetWorth_requestsCryptoHoldingsInBaseCurrency() {
        this.netWorthService.getNetWorth(USER, "HUF", false);

        ArgumentCaptor<TransactionQuery> captor = ArgumentCaptor.forClass(TransactionQuery.class);
        verify(this.transactionService).getCurrentHoldings(eq(USER), captor.capture());

        assertEquals("EUR", captor.getValue().getCurrency());
    }

    @Test
    void getNetWorth_fallsBackToCostWhenNoLivePriceIsAvailable() {
        when(this.etfInvestmentService.getCurrentETFHoldings(USER)).thenReturn(List.of(
                ETFInvestmentDTO.builder().amount(500.0).liveValue(null).currencyId("EUR").build()));

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR", false);

        assertEquals(0, this.assetOf(result, NetWorthService.ETF).getWorth().compareTo(
                new BigDecimal("500.00")));
    }

    @Test
    void getNetWorth_valuesForexHoldingsInTheCurrencySoldFrom() {
        when(this.forexTransactionService.getAllForexHoldings(USER)).thenReturn(List.of(
                ForexTransactionDTO.builder().fromAmount(1000.0).liveValue(1100.0)
                        .fromCurrencyId("EUR").toCurrencyId("USD").build()));

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR", false);

        assertEquals(0, this.assetOf(result, NetWorthService.FOREX).getWorth().compareTo(
                new BigDecimal("1100.00")));
    }

    /**
     * A coupon bond redeems at par, so par is what it is worth. It is still not a gain or a loss: the same
     * arithmetic that turns a discount into a gain turns the accrued interest paid on a mid-period purchase
     * into a loss on a holding whose value never moved. Interest stays out of it either way - it is paid out
     * and reinvested by buying more, and those purchases already show up as transactions, so counting it
     * here would count the same money twice.
     */
    @Test
    void getNetWorth_reportsNoChangeOnSecuritiesEvenWhenParDiffersFromCost() {
        when(this.securityTransactionService.getCurrentHoldings(USER)).thenReturn(List.of(
                SecurityTransactionDTO.builder().amount(950.0).quantity(1000).currencyId("EUR").build()));

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR", false);

        AssetClassValueDTO securities = this.assetOf(result, NetWorthService.SECURITIES);
        assertEquals(0, securities.getSpent().compareTo(new BigDecimal("950.00")));
        assertEquals(0, securities.getWorth().compareTo(new BigDecimal("1000.00")));
        assertEquals(0, securities.getChangePct().signum());
    }

    /**
     * Securities weigh on the total as money invested that did not move, so they pull the percentage towards
     * zero rather than contributing a gain or a loss of their own.
     */
    @Test
    void getNetWorth_leavesSecuritiesOutOfTheTotalChangeButNotOutOfWhatWasSpent() {
        when(this.investmentService.getCurrentHoldings(USER)).thenReturn(List.of(
                InvestmentDTO.builder().amount(1_000_000.0).liveValue(1_200_000.0).currencyId("EUR").build()));
        when(this.securityTransactionService.getCurrentHoldings(USER)).thenReturn(List.of(
                SecurityTransactionDTO.builder().amount(10_200_000.0).quantity(10_000_000).currencyId("EUR").build()));

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR", false);

        // Only the stock moved: 200K gain on the 11.2M spent across both. Counting the 200K the bond is
        // under par would have shown 0.00, and dropping the bond from the denominator would have shown 20.00.
        assertEquals(0, result.getTotalChangePct().compareTo(new BigDecimal("1.79")));
        assertEquals(0, result.getTotalSpent().compareTo(new BigDecimal("11200000.00")));
        assertEquals(0, result.getTotalWorth().compareTo(new BigDecimal("11200000.00")));
    }

    /**
     * A zero coupon bond's par already contains all the interest it has yet to earn, so valuing it at par
     * would book an unearned gain. It is worth what it cost, and that cost is also what weights its rate.
     */
    @Test
    void getNetWorth_valuesZeroCouponSecuritiesAtCostNotAtPar() {
        when(this.securityTransactionService.getCurrentHoldings(USER)).thenReturn(List.of(
                SecurityTransactionDTO.builder().amount(950_000.0).quantity(1_000_000).rate(5.0)
                        .currencyId("EUR").build(),
                SecurityTransactionDTO.builder().amount(800_000.0).quantity(1_000_000).rate(25.0)
                        .zeroCoupon(true).currencyId("EUR").build()));

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR", false);

        AssetClassValueDTO securities = this.assetOf(result, NetWorthService.SECURITIES);
        assertEquals(0, securities.getSpent().compareTo(new BigDecimal("1750000.00")));
        // 1M par for the coupon bond, 800K cost for the zero coupon one
        assertEquals(0, securities.getWorth().compareTo(new BigDecimal("1800000.00")));
        // (1M x 5.0 + 800K x 25.0) / 1.8M; weighting the zero coupon bond by its par would have given 15.0
        assertEquals(0, securities.getExpectedRatePct().compareTo(new BigDecimal("13.89")));
        assertEquals(0, result.getTotalWorth().compareTo(new BigDecimal("1800000.00")));
    }

    /**
     * A big position has to pull the average towards its own rate; the plain average of 5.5 and 5.0 would be
     * 5.25, which would let the 1M holding count as much as the 17M one.
     */
    @Test
    void getNetWorth_weightsTheSecuritiesRateByQuantityNotByHoldingCount() {
        when(this.securityTransactionService.getCurrentHoldings(USER)).thenReturn(List.of(
                SecurityTransactionDTO.builder().amount(16_000_000.0).quantity(17_000_000).rate(5.5).currencyId("EUR").build(),
                SecurityTransactionDTO.builder().amount(950_000.0).quantity(1_000_000).rate(5.0).currencyId("EUR").build()));

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR", false);

        // (17M x 5.5 + 1M x 5.0) / 18M
        assertEquals(0, this.assetOf(result, NetWorthService.SECURITIES).getExpectedRatePct()
                .compareTo(new BigDecimal("5.47")));
    }

    @Test
    void getNetWorth_countsSecuritiesWithNoRateAsZeroPercent() {
        when(this.securityTransactionService.getCurrentHoldings(USER)).thenReturn(List.of(
                SecurityTransactionDTO.builder().amount(16_000_000.0).quantity(17_000_000).rate(5.5).currencyId("EUR").build(),
                SecurityTransactionDTO.builder().amount(950_000.0).quantity(1_000_000).rate(5.0).currencyId("EUR").build(),
                SecurityTransactionDTO.builder().amount(4_800_000.0).quantity(5_000_000).rate(null).currencyId("EUR").build()));

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR", false);

        // The rateless 5M stays in the denominator: (17M x 5.5 + 1M x 5.0 + 0) / 23M
        assertEquals(0, this.assetOf(result, NetWorthService.SECURITIES).getExpectedRatePct()
                .compareTo(new BigDecimal("4.28")));
    }

    @Test
    void getNetWorth_convertsQuantitiesBeforeWeightingTheSecuritiesRate() {
        when(this.securityTransactionService.getCurrentHoldings(USER)).thenReturn(List.of(
                SecurityTransactionDTO.builder().amount(1_000_000.0).quantity(1_000_000).rate(10.0).currencyId("EUR").build(),
                SecurityTransactionDTO.builder().amount(400_000_000.0).quantity(400_000_000).rate(5.0).currencyId("HUF").build()));

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR", false);

        // 400M HUF / 400 = 1M EUR, so the two weigh the same: (10.0 + 5.0) / 2. Weighting the raw HUF figure
        // would have given 5.01.
        assertEquals(0, this.assetOf(result, NetWorthService.SECURITIES).getExpectedRatePct()
                .compareTo(new BigDecimal("7.50")));
    }

    @Test
    void getNetWorth_reportsZeroExpectedRateWhenNoSecuritiesAreHeld() {
        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR", false);

        assertEquals(0, this.assetOf(result, NetWorthService.SECURITIES).getExpectedRatePct().signum());
    }

    @Test
    void getNetWorth_convertsCashIntoTheTargetCurrency() {
        this.stubCash(400_000.0);

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR", false);

        AssetClassValueDTO cash = this.assetOf(result, NetWorthService.CASH);
        assertEquals(0, cash.getWorth().compareTo(new BigDecimal("1000.00")));
        assertEquals(0, cash.getSpent().compareTo(new BigDecimal("1000.00")));
        assertEquals(0, cash.getChangePct().signum());
        assertEquals(0, result.getTotalWorth().compareTo(new BigDecimal("1000.00")));
    }

    @Test
    void getNetWorth_looksUpRatesWhenCashIsTheOnlyHolding() {
        this.stubCash(400_000.0);

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR", false);

        assertTrue(result.getUnconvertedCurrencies().isEmpty());
        assertEquals(0, result.getTotalWorth().compareTo(new BigDecimal("1000.00")));
    }

    @Test
    void getNetWorth_leavesCashOutOfTheTotalChangeOnBothSides() {
        this.stubCash(400_000.0);
        when(this.investmentService.getCurrentHoldings(USER)).thenReturn(List.of(
                InvestmentDTO.builder().amount(1000.0).liveValue(1200.0).currencyId("EUR").build()));

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR", false);

        assertEquals(0, result.getTotalChangePct().compareTo(new BigDecimal("20.00")));
        assertEquals(0, result.getTotalSpent().compareTo(new BigDecimal("2000.00")));
        assertEquals(0, result.getTotalWorth().compareTo(new BigDecimal("2200.00")));
    }

    @Test
    void getNetWorth_reportsCurrenciesItCouldNotConvertRatherThanCountingThemAsZero() {
        this.stubRates(Map.of("USD", 1.10));
        when(this.investmentService.getCurrentHoldings(USER)).thenReturn(List.of(
                InvestmentDTO.builder().amount(100.0).liveValue(100.0).currencyId("GBP").build()));

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR", false);

        assertEquals(List.of("GBP"), result.getUnconvertedCurrencies());
        assertEquals(0, result.getTotalWorth().signum());
    }

    @Test
    void getNetWorth_listsEveryCurrencyHeldSoTheUiCanOfferThem() {
        when(this.investmentService.getCurrentHoldings(USER)).thenReturn(List.of(
                InvestmentDTO.builder().amount(10.0).liveValue(10.0).currencyId("USD").build()));

        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "HUF", false);

        assertTrue(result.getAvailableCurrencies().containsAll(List.of("EUR", "HUF", "USD")));
    }

    @Test
    void getNetWorth_defaultsToBaseCurrencyWhenNoneIsGiven() {
        NetWorthDTO result = this.netWorthService.getNetWorth(USER, null, false);

        assertEquals("EUR", result.getCurrency());
    }

    @Test
    void getNetWorth_throwsWhenTheTargetCurrencyHasNoRate() {
        this.stubRates(Map.of("USD", 1.10));
        when(this.investmentService.getCurrentHoldings(USER)).thenReturn(List.of(
                InvestmentDTO.builder().amount(100.0).liveValue(150.0).currencyId("USD").build()));

        assertThrows(APIException.class, () -> this.netWorthService.getNetWorth(USER, "JPY", false));
    }

    @Test
    void getNetWorth_doesNotLookUpRatesWhenNothingIsHeld() {
        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "HUF", false);

        assertEquals(0, result.getTotalWorth().signum());
        assertEquals(0, result.getTotalSpent().signum());
        verifyNoInteractions(this.dashboardService);
    }

    @Test
    void getNetWorth_reportsZeroChangeWhenNothingWasSpent() {
        NetWorthDTO result = this.netWorthService.getNetWorth(USER, "EUR", false);

        assertEquals(0, result.getTotalSpent().signum());
        assertEquals(0, result.getTotalWorth().signum());
        assertEquals(0, result.getTotalChangePct().signum());
    }
}
