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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RequiredArgsConstructor
public class NetWorthService {

    public static final String STOCK = "Stock";
    public static final String CRYPTO = "Crypto";
    public static final String ETF = "ETF";
    public static final String FOREX = "Forex";
    public static final String SECURITIES = "Securities";

    private static final String BASE_CURRENCY = "EUR";
    private static final int SCALE = 2;

    private final InvestmentService investmentService;
    private final TransactionService transactionService;
    private final ETFInvestmentService etfInvestmentService;
    private final ForexTransactionService forexTransactionService;
    private final SecurityTransactionService securityTransactionService;
    private final DashboardService dashboardService;

    public NetWorthDTO getNetWorth(String userEmail, String currency, boolean refresh) {
        log.trace("Enter");
        String target = (currency == null || currency.isBlank()) ? BASE_CURRENCY : currency.toUpperCase();

        Holdings holdings = this.loadHoldings(userEmail, refresh);
        Set<String> currencies = this.currenciesIn(holdings, target);
        Map<String, Double> rates = holdings.isEmpty() ? Map.of()
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
                this.securities(holdings.securities(), converter));

        double spent = assets.stream().mapToDouble(asset -> asset.getSpent().doubleValue()).sum();
        double worth = assets.stream().mapToDouble(asset -> asset.getWorth().doubleValue()).sum();
        double gain = assets.stream()
                .filter(asset -> !SECURITIES.equals(asset.getAssetClass()))
                .mapToDouble(asset -> asset.getWorth().doubleValue() - asset.getSpent().doubleValue())
                .sum();

        return NetWorthDTO.builder()
                .currency(target)
                .totalSpent(this.scaled(spent))
                .totalWorth(this.scaled(worth))
                .totalChangePct(this.gainPct(gain, spent))
                .assets(assets)
                .availableCurrencies(new ArrayList<>(currencies))
                .unconvertedCurrencies(new ArrayList<>(converter.unconverted()))
                .build();
    }

    private Holdings loadHoldings(String userEmail, boolean refresh) {
        TransactionQuery cryptoQuery = TransactionQuery.builder().currency(BASE_CURRENCY).build();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<List<InvestmentDTO>> stock = this.async(executor,
                    () -> refresh ? this.investmentService.refreshCurrentHoldings(userEmail)
                            : this.investmentService.getCurrentHoldings(userEmail));
            CompletableFuture<List<TransactionDTO>> crypto = this.async(executor,
                    () -> refresh ? this.transactionService.refreshCurrentHoldings(userEmail, cryptoQuery)
                            : this.transactionService.getCurrentHoldings(userEmail, cryptoQuery));
            CompletableFuture<List<ETFInvestmentDTO>> etf = this.async(executor,
                    () -> refresh ? this.etfInvestmentService.refreshCurrentETFHoldings(userEmail)
                            : this.etfInvestmentService.getCurrentETFHoldings(userEmail));
            CompletableFuture<List<ForexTransactionDTO>> forex = this.async(executor,
                    () -> refresh ? this.forexTransactionService.refreshAllForexHoldings(userEmail)
                            : this.forexTransactionService.getAllForexHoldings(userEmail));
            CompletableFuture<List<SecurityTransactionDTO>> securities = this.async(executor,
                    () -> this.securityTransactionService.getCurrentHoldings(userEmail));

            return new Holdings(this.join(stock), this.join(crypto), this.join(etf), this.join(forex),
                    this.join(securities));
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
                                         Function<T, Double> cost, Function<T, String> costCurrency,
                                         Function<T, Double> live, Function<T, String> liveCurrency,
                                         Converter converter) {
        double spent = 0;
        double worth = 0;
        for (T item : items) {
            spent += converter.convert(cost.apply(item), costCurrency.apply(item));
            Double liveValue = live.apply(item);
            // No live value means the price lookup failed; fall back to cost so the holding does not vanish.
            worth += (liveValue != null)
                    ? converter.convert(liveValue, liveCurrency.apply(item))
                    : converter.convert(cost.apply(item), costCurrency.apply(item));
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
        double spent = 0;
        double worth = 0;
        double weighted = 0;
        for (SecurityTransactionDTO transaction : transactions) {
            spent += converter.convert(transaction.getAmount(), transaction.getCurrencyId());
            double converted = converter.convert(this.faceValue(transaction), transaction.getCurrencyId());
            worth += converted;
            if (transaction.getRate() != null) weighted += transaction.getRate() * converted;
        }
        AssetClassValueDTO asset = this.asset(SECURITIES, spent, worth);
        asset.setChangePct(this.scaled(0));
        asset.setExpectedRatePct(this.scaled(worth == 0 ? 0 : weighted / worth));
        return asset;
    }

    private Double faceValue(SecurityTransactionDTO transaction) {
        if (Boolean.TRUE.equals(transaction.getZeroCoupon()) || transaction.getQuantity() == null) {
            return transaction.getAmount();
        }
        return transaction.getQuantity().doubleValue();
    }

    private AssetClassValueDTO asset(String assetClass, double spent, double worth) {
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
        return currencies;
    }

    private void add(Set<String> currencies, String currency) {
        if (currency != null && !currency.isBlank()) currencies.add(currency.toUpperCase());
    }

    private BigDecimal scaled(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal changePct(double spent, double worth) {
        return this.gainPct(worth - spent, spent);
    }

    private BigDecimal gainPct(double gain, double spent) {
        if (spent == 0) return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(gain / spent * 100).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private record Holdings(List<InvestmentDTO> stock, List<TransactionDTO> crypto, List<ETFInvestmentDTO> etf,
                            List<ForexTransactionDTO> forex, List<SecurityTransactionDTO> securities) {

        private boolean isEmpty() {
            return this.stock.isEmpty() && this.crypto.isEmpty() && this.etf.isEmpty()
                    && this.forex.isEmpty() && this.securities.isEmpty();
        }
    }

    /**
     * Converts between currencies using EUR-based rates, where a rate is how many units of that currency one
     * euro buys. Anything it cannot convert is dropped from the total and recorded, rather than silently
     * counted as zero.
     */
    private static final class Converter {

        private final Map<String, Double> rates;
        private final String target;
        private final Set<String> unconverted = new TreeSet<>();

        private Converter(Map<String, Double> rates, String target) {
            this.rates = rates;
            this.target = target;
        }

        private double convert(Double amount, String from) {
            if (amount == null || amount == 0) return 0;
            if (from == null || from.isBlank()) {
                this.unconverted.add("(unknown)");
                return 0;
            }
            double fromRate = this.rateFor(from.toUpperCase());
            if (fromRate == 0) {
                this.unconverted.add(from.toUpperCase());
                return 0;
            }
            return amount / fromRate * this.targetRate();
        }

        private double targetRate() {
            double rate = this.rateFor(this.target);
            if (rate == 0) {
                throw new APIException("No exchange rate available for " + this.target);
            }
            return rate;
        }

        private double rateFor(String currency) {
            if (BASE_CURRENCY.equals(currency)) return 1;
            Double rate = this.rates.get(currency);
            return (rate == null || rate == 0) ? 0 : rate;
        }

        private Set<String> unconverted() {
            return this.unconverted;
        }
    }
}
