package eye.on.the.money.service.shared;

import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.AssetClassValueDTO;
import eye.on.the.money.dto.out.CashDTO;
import eye.on.the.money.dto.out.DashboardRatesDTO;
import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.dto.out.ForexTransactionDTO;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.dto.out.NetWorthDTO;
import eye.on.the.money.dto.out.PensionDTO;
import eye.on.the.money.dto.out.SecurityTransactionDTO;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.service.cash.CashService;
import eye.on.the.money.service.crypto.TransactionService;
import eye.on.the.money.service.etf.ETFInvestmentService;
import eye.on.the.money.service.forex.ForexTransactionService;
import eye.on.the.money.service.pension.PensionService;
import eye.on.the.money.service.security.SecurityTransactionService;
import eye.on.the.money.service.stock.InvestmentService;
import eye.on.the.money.util.Numbers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Values everything a user holds in one currency.
 * <p>
 * Each asset class reports its cost and its live worth in its own currency, so the totals are only meaningful
 * once every row has been put through the same FX conversion. Rates come from
 * {@link DashboardService#getConversionRates}, which quotes everything against EUR.
 * <p>
 * A note on crypto: {@code liveValue} is denominated in the currency the holdings were <em>requested</em> in
 * (this service always asks for EUR), not in the {@code currencyId} the coins were bought with. Those two
 * differ for anyone who paid in something other than EUR, so the live figure is converted from EUR while the
 * cost basis is converted from {@code currencyId}.
 */
@Service
@RequiredArgsConstructor
public class NetWorthService {

    public static final String STOCK = "Stock";
    public static final String CRYPTO = "Crypto";
    public static final String ETF = "ETF";
    public static final String FOREX = "Forex";
    public static final String SECURITIES = "Securities";
    public static final String CASH = "Cash";
    public static final String PENSION = "Pension";

    private static final String BASE_CURRENCY = "EUR";
    private static final int SCALE = 2;

    private final InvestmentService investmentService;
    private final TransactionService transactionService;
    private final ETFInvestmentService etfInvestmentService;
    private final ForexTransactionService forexTransactionService;
    private final SecurityTransactionService securityTransactionService;
    private final CashService cashService;
    private final PensionService pensionService;
    private final DashboardService dashboardService;

    public NetWorthDTO getNetWorth(Long userId, String currency, boolean refresh) {
        String target = (currency == null || currency.isBlank()) ? BASE_CURRENCY : currency.toUpperCase();

        Holdings holdings = this.loadHoldings(userId, refresh);
        Set<String> currencies = this.currenciesIn(holdings, target);
        Map<String, BigDecimal> rates = holdings.isEmpty() ? Map.of()
                : this.conversionRates(new ArrayList<>(currencies), refresh).getRates();
        Converter converter = new Converter(rates, target);

        List<AssetClassValueDTO> assets = List.of(
                this.value(STOCK, holdings.stock(), InvestmentDTO::getAmount, InvestmentDTO::getCurrencyId,
                        InvestmentDTO::getLiveValue, InvestmentDTO::getCurrencyId, converter),
                this.value(CRYPTO, holdings.crypto(), TransactionDTO::getAmount, TransactionDTO::getCurrencyId,
                        TransactionDTO::getLiveValue, _ -> BASE_CURRENCY, converter),
                this.value(ETF, holdings.etf(), ETFInvestmentDTO::getAmount, ETFInvestmentDTO::getCurrencyId,
                        ETFInvestmentDTO::getLiveValue, ETFInvestmentDTO::getCurrencyId, converter),
                this.value(FOREX, holdings.forex(), ForexTransactionDTO::getFromAmount,
                        ForexTransactionDTO::getFromCurrencyId, ForexTransactionDTO::getLiveValue,
                        ForexTransactionDTO::getFromCurrencyId, converter),
                this.securities(holdings.securities(), converter),
                this.cash(holdings.cash(), converter),
                this.pension(holdings.pension(), converter));

        BigDecimal spent = assets.stream().map(AssetClassValueDTO::getSpent).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal worth = assets.stream().map(AssetClassValueDTO::getWorth).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gainBase = assets.stream()
                .filter(asset -> !CASH.equals(asset.getAssetClass()))
                .map(AssetClassValueDTO::getSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gain = assets.stream()
                .filter(asset -> !SECURITIES.equals(asset.getAssetClass()) && !CASH.equals(asset.getAssetClass()))
                .map(asset -> asset.getWorth().subtract(asset.getSpent()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return NetWorthDTO.builder()
                .currency(target)
                .totalSpent(this.scaled(spent))
                .totalWorth(this.scaled(worth))
                .totalChangePct(this.gainPct(gain, gainBase))
                .assets(assets)
                .availableCurrencies(new ArrayList<>(currencies))
                .unconvertedCurrencies(new ArrayList<>(converter.unconverted()))
                .build();
    }

    private Holdings loadHoldings(Long userId, boolean refresh) {
        TransactionQuery cryptoQuery = TransactionQuery.builder().currency(BASE_CURRENCY).build();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<List<InvestmentDTO>> stock = this.async(executor,
                    () -> refresh ? this.investmentService.refreshCurrentHoldings(userId)
                            : this.investmentService.getCurrentHoldings(userId));
            CompletableFuture<List<TransactionDTO>> crypto = this.async(executor,
                    () -> refresh ? this.transactionService.refreshCurrentHoldings(userId, cryptoQuery)
                            : this.transactionService.getCurrentHoldings(userId, cryptoQuery));
            CompletableFuture<List<ETFInvestmentDTO>> etf = this.async(executor,
                    () -> refresh ? this.etfInvestmentService.refreshCurrentETFHoldings(userId)
                            : this.etfInvestmentService.getCurrentETFHoldings(userId));
            CompletableFuture<List<ForexTransactionDTO>> forex = this.async(executor,
                    () -> refresh ? this.forexTransactionService.refreshAllForexHoldings(userId)
                            : this.forexTransactionService.getAllForexHoldings(userId));
            CompletableFuture<List<SecurityTransactionDTO>> securities = this.async(executor,
                    () -> this.securityTransactionService.getCurrentHoldings(userId));
            CompletableFuture<CashDTO> cash = this.async(executor, () -> this.cashService.getCash(userId));
            CompletableFuture<PensionDTO> pension = this.async(executor,
                    () -> this.pensionService.getPension(userId));

            return new Holdings(this.join(stock), this.join(crypto), this.join(etf), this.join(forex),
                    this.join(securities), this.join(cash), this.join(pension));
        }
    }

    private <T> CompletableFuture<T> async(ExecutorService executor, Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    private <T> T join(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) throw cause;
            if (e.getCause() instanceof Error cause) throw cause;
            throw e;
        }
    }

    private DashboardRatesDTO conversionRates(List<String> currencies, boolean refresh) {
        return refresh ? this.dashboardService.refreshConversionRates(currencies)
                : this.dashboardService.getConversionRates(currencies);
    }

    /**
     * Sums one asset class. The live value carries its own currency because crypto quotes it in EUR while
     * everything else quotes it in the currency the position was opened in.
     */
    private <T> AssetClassValueDTO value(String assetClass, List<T> items,
                                         Function<T, BigDecimal> cost, Function<T, String> costCurrency,
                                         Function<T, BigDecimal> live, Function<T, String> liveCurrency,
                                         Converter converter) {
        BigDecimal spent = BigDecimal.ZERO;
        BigDecimal worth = BigDecimal.ZERO;
        for (T item : items) {
            spent = spent.add(converter.convert(cost.apply(item), costCurrency.apply(item)));
            BigDecimal liveValue = live.apply(item);
            // No live value means the price lookup failed; fall back to cost so the holding does not vanish.
            worth = worth.add((liveValue != null)
                    ? converter.convert(liveValue, liveCurrency.apply(item))
                    : converter.convert(cost.apply(item), costCurrency.apply(item)));
        }
        return this.asset(assetClass, spent, worth);
    }

    /**
     * Securities have no live price. A coupon bond is valued at par ({@code quantity}), which is what it
     * redeems for; a zero-coupon bond is valued at cost ({@code amount}), because its par already contains
     * all the interest it has yet to earn and booking that now would be an unearned gain. Interest itself
     * stays out of what they are worth either way: it is paid out and then reinvested by buying more, and
     * those purchases are already recorded as transactions, so counting it here would count the same money
     * twice.
     */
    private AssetClassValueDTO securities(List<SecurityTransactionDTO> transactions, Converter converter) {
        BigDecimal spent = BigDecimal.ZERO;
        BigDecimal worth = BigDecimal.ZERO;
        BigDecimal weighted = BigDecimal.ZERO;
        for (SecurityTransactionDTO transaction : transactions) {
            spent = spent.add(converter.convert(transaction.getAmount(), transaction.getCurrencyId()));
            BigDecimal converted = converter.convert(this.faceValue(transaction), transaction.getCurrencyId());
            worth = worth.add(converted);
            if (transaction.getRate() != null) weighted = weighted.add(transaction.getRate().multiply(converted));
        }
        AssetClassValueDTO asset = this.asset(SECURITIES, spent, worth);
        asset.setChangePct(this.scaled(BigDecimal.ZERO));
        asset.setExpectedRatePct(this.scaled(worth.signum() == 0 ? BigDecimal.ZERO : Numbers.divide(weighted, worth)));
        return asset;
    }

    private BigDecimal faceValue(SecurityTransactionDTO transaction) {
        if (Boolean.TRUE.equals(transaction.getZeroCoupon()) || transaction.getQuantity() == null) {
            return transaction.getAmount();
        }
        return BigDecimal.valueOf(transaction.getQuantity());
    }

    private AssetClassValueDTO cash(CashDTO cash, Converter converter) {
        BigDecimal converted = (cash == null) ? BigDecimal.ZERO : converter.convert(cash.getAmount(), cash.getCurrency());
        AssetClassValueDTO asset = this.asset(CASH, converted, converted);
        asset.setChangePct(this.scaled(BigDecimal.ZERO));
        return asset;
    }

    private AssetClassValueDTO pension(PensionDTO pension, Converter converter) {
        if (pension == null) return this.asset(PENSION, BigDecimal.ZERO, BigDecimal.ZERO);
        BigDecimal spent = converter.convert(pension.getTotalContribution(), pension.getCurrency());
        BigDecimal worth = converter.convert(pension.getCurrentValue(), pension.getCurrency());
        return this.asset(PENSION, spent, worth);
    }

    private AssetClassValueDTO asset(String assetClass, BigDecimal spent, BigDecimal worth) {
        return AssetClassValueDTO.builder()
                .assetClass(assetClass)
                .spent(this.scaled(spent))
                .worth(this.scaled(worth))
                .changePct(this.changePct(spent, worth))
                .build();
    }

    private Set<String> currenciesIn(Holdings holdings, String target) {
        Set<String> currencies = new TreeSet<>();
        currencies.add(BASE_CURRENCY);
        currencies.add(target);
        holdings.stock().forEach(item -> this.add(currencies, item.getCurrencyId()));
        holdings.crypto().forEach(item -> this.add(currencies, item.getCurrencyId()));
        holdings.etf().forEach(item -> this.add(currencies, item.getCurrencyId()));
        holdings.forex().forEach(item -> {
            this.add(currencies, item.getFromCurrencyId());
            this.add(currencies, item.getToCurrencyId());
        });
        holdings.securities().forEach(item -> this.add(currencies, item.getCurrencyId()));
        if (holdings.hasCash()) this.add(currencies, holdings.cash().getCurrency());
        if (holdings.hasPension()) this.add(currencies, holdings.pension().getCurrency());
        return currencies;
    }

    private void add(Set<String> currencies, String currency) {
        if (currency != null && !currency.isBlank()) currencies.add(currency.toUpperCase());
    }

    private BigDecimal scaled(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal changePct(BigDecimal spent, BigDecimal worth) {
        return this.gainPct(worth.subtract(spent), spent);
    }

    private BigDecimal gainPct(BigDecimal gain, BigDecimal spent) {
        if (spent.signum() == 0) return this.scaled(BigDecimal.ZERO);
        return this.scaled(Numbers.divide(gain, spent).multiply(Numbers.HUNDRED));
    }

    private record Holdings(List<InvestmentDTO> stock, List<TransactionDTO> crypto, List<ETFInvestmentDTO> etf,
                            List<ForexTransactionDTO> forex, List<SecurityTransactionDTO> securities,
                            CashDTO cash, PensionDTO pension) {

        private boolean isEmpty() {
            return this.stock.isEmpty() && this.crypto.isEmpty() && this.etf.isEmpty()
                    && this.forex.isEmpty() && this.securities.isEmpty() && !this.hasCash()
                    && !this.hasPension();
        }

        private boolean hasCash() {
            return this.cash != null && this.nonZero(this.cash.getAmount());
        }

        private boolean hasPension() {
            return this.pension != null
                    && (this.nonZero(this.pension.getTotalContribution())
                    || this.nonZero(this.pension.getCurrentValue()));
        }

        private boolean nonZero(BigDecimal value) {
            return value != null && value.signum() != 0;
        }
    }

    /**
     * Converts between currencies using EUR-based rates, where a rate is how many units of that currency one
     * euro buys. Anything it cannot convert is dropped from the total and recorded, rather than silently
     * counted as zero.
     */
    private static final class Converter {

        private final Map<String, BigDecimal> rates;
        private final String target;
        private final Set<String> unconverted = new TreeSet<>();

        private Converter(Map<String, BigDecimal> rates, String target) {
            this.rates = rates;
            this.target = target;
        }

        private BigDecimal convert(BigDecimal amount, String from) {
            if (amount == null || amount.signum() == 0) return BigDecimal.ZERO;
            if (from == null || from.isBlank()) {
                this.unconverted.add("(unknown)");
                return BigDecimal.ZERO;
            }
            BigDecimal fromRate = this.rateFor(from.toUpperCase());
            if (fromRate.signum() == 0) {
                this.unconverted.add(from.toUpperCase());
                return BigDecimal.ZERO;
            }
            return Numbers.divide(amount.multiply(this.targetRate()), fromRate);
        }

        private BigDecimal targetRate() {
            BigDecimal rate = this.rateFor(this.target);
            if (rate.signum() == 0) {
                throw new APIException("No exchange rate available for " + this.target);
            }
            return rate;
        }

        private BigDecimal rateFor(String currency) {
            if (BASE_CURRENCY.equals(currency)) return BigDecimal.ONE;
            BigDecimal rate = this.rates.get(currency);
            return rate == null ? BigDecimal.ZERO : rate;
        }

        private Set<String> unconverted() {
            return this.unconverted;
        }
    }
}
