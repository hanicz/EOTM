package eye.on.the.money.service.shared;

import eye.on.the.money.dto.in.RSUDTO;
import eye.on.the.money.dto.out.RSUTaxDTO;
import eye.on.the.money.dto.out.TaxBreakdownDTO;
import eye.on.the.money.dto.out.TaxReportDTO;
import eye.on.the.money.exception.TaxException;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.service.api.EODAPIService;
import eye.on.the.money.service.api.MNBAPIService;
import eye.on.the.money.service.stock.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Works out the tax on RSUs. There is nothing to match against - the shares were granted, not bought - so
 * each row is simply valued at the closing price on its date, converted to HUF at that day's MNB rate and
 * put through the tax method.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TaxService {

    /** MNB and the exchanges both skip non-trading days; look back far enough to clear a long holiday. */
    private static final int LOOKBACK_DAYS = 14;

    private static final String DEFAULT_EXCHANGE = "US";

    private final EODAPIService eodAPIService;
    private final MNBAPIService mnbAPIService;
    private final StockService stockService;
    private final TaxCalculator taxCalculator;

    public TaxBreakdownDTO calculateTax(BigDecimal amountInHuf) {
        log.trace("Enter");
        return this.taxCalculator.calculateTax(amountInHuf);
    }

    public TaxReportDTO calculateTaxForRSUs(List<RSUDTO> rsus) {
        log.trace("Enter");
        if (rsus == null || rsus.isEmpty()) {
            return TaxReportDTO.builder().items(List.of())
                    .totalAmountInHuf(BigDecimal.ZERO).totalTax(TaxBreakdownDTO.zero()).build();
        }

        Map<String, String> currencies = this.resolveCurrencies(rsus);
        Map<String, PriceSeries> prices = this.fetchPrices(rsus);
        Map<String, NavigableMap<LocalDate, BigDecimal>> rates = this.fetchRates(rsus, currencies);

        List<RSUTaxDTO> items = new ArrayList<>();
        for (RSUDTO rsu : rsus) {
            items.add(this.calculateRSU(rsu, currencies, prices, rates));
        }

        BigDecimal totalAmount = items.stream().map(RSUTaxDTO::getAmountInHuf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        TaxBreakdownDTO totalTax = items.stream().map(RSUTaxDTO::getTax)
                .reduce(TaxBreakdownDTO.zero(), TaxBreakdownDTO::plus);

        return TaxReportDTO.builder().items(items).totalAmountInHuf(totalAmount).totalTax(totalTax).build();
    }

    private RSUTaxDTO calculateRSU(RSUDTO rsu, Map<String, String> currencies,
                                   Map<String, PriceSeries> prices,
                                   Map<String, NavigableMap<LocalDate, BigDecimal>> rates) {
        String ticker = this.ticker(rsu);
        String currency = currencies.get(ticker);

        Map.Entry<LocalDate, BigDecimal> close = prices.get(ticker).closeOn(rsu.getDate());
        if (close == null) {
            throw new TaxException("No closing price available for " + ticker + " on or before " + rsu.getDate());
        }

        BigDecimal amount = close.getValue().multiply(BigDecimal.valueOf(rsu.getQuantity()));
        Map.Entry<LocalDate, BigDecimal> rate = this.rateOn(rates, currency, rsu.getDate());
        BigDecimal amountInHuf = amount.multiply(rate.getValue()).setScale(2, RoundingMode.HALF_UP);

        return RSUTaxDTO.builder()
                .shortName(rsu.getShortName())
                .exchange(this.exchange(rsu))
                .date(rsu.getDate())
                .quantity(rsu.getQuantity())
                .currency(currency)
                .price(close.getValue())
                .priceDate(close.getKey())
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .rate(rate.getValue())
                .rateDate(rate.getKey())
                .amountInHuf(amountInHuf)
                .tax(this.taxCalculator.calculateTax(amountInHuf))
                .build();
    }


    private Map<String, String> resolveCurrencies(List<RSUDTO> rsus) {
        Map<String, String> byExchange = null;
        Map<String, String> currencies = new HashMap<>();

        for (RSUDTO rsu : rsus) {
            String ticker = this.ticker(rsu);
            if (currencies.containsKey(ticker)) continue;

            if (rsu.getCurrency() != null && !rsu.getCurrency().isBlank()) {
                currencies.put(ticker, rsu.getCurrency().toUpperCase());
                continue;
            }
            if (byExchange == null) {
                byExchange = this.stockService.getAllExchanges().stream()
                        .filter(exchange -> exchange.getCode() != null && exchange.getCurrency() != null)
                        .collect(Collectors.toMap(Exchange::getCode, Exchange::getCurrency, (first, second) -> first));
            }

            String currency = byExchange.get(this.exchange(rsu));
            if (currency == null) {
                throw new TaxException("Unknown currency for exchange " + this.exchange(rsu)
                        + "; set it explicitly on the request");
            }
            currencies.put(ticker, currency.toUpperCase());
        }
        return currencies;
    }

    private Map<String, PriceSeries> fetchPrices(List<RSUDTO> rsus) {
        Map<String, PriceSeries> prices = new HashMap<>();
        rsus.stream().collect(Collectors.groupingBy(this::ticker,
                        Collectors.mapping(RSUDTO::getDate, Collectors.toList())))
                .forEach((ticker, dates) -> {
                    LocalDate from = dates.stream().min(LocalDate::compareTo).orElseThrow().minusDays(LOOKBACK_DAYS);
                    LocalDate to = dates.stream().max(LocalDate::compareTo).orElseThrow();

                    NavigableMap<LocalDate, BigDecimal> closes = new TreeMap<>();
                    this.eodAPIService.getHistoricalQuotes(ticker, from, to).stream()
                            .filter(quote -> quote.getDate() != null && quote.getClose() != null)
                            .forEach(quote -> closes.put(quote.getDate(), BigDecimal.valueOf(quote.getClose())));
                    prices.put(ticker, new PriceSeries(closes));
                });
        return prices;
    }

    private Map<String, NavigableMap<LocalDate, BigDecimal>> fetchRates(List<RSUDTO> rsus,
                                                                        Map<String, String> currencies) {
        Set<String> needed = new HashSet<>(currencies.values());
        if (needed.stream().allMatch(MNBAPIService.HUF::equalsIgnoreCase)) return Map.of();

        LocalDate earliest = rsus.stream().map(RSUDTO::getDate).min(LocalDate::compareTo).orElseThrow();
        LocalDate latest = rsus.stream().map(RSUDTO::getDate).max(LocalDate::compareTo).orElseThrow();

        return this.mnbAPIService.getExchangeRates(needed, earliest.minusDays(LOOKBACK_DAYS), latest);
    }

    private Map.Entry<LocalDate, BigDecimal> rateOn(Map<String, NavigableMap<LocalDate, BigDecimal>> rates,
                                                    String currency, LocalDate date) {
        if (MNBAPIService.HUF.equalsIgnoreCase(currency)) return Map.entry(date, BigDecimal.ONE);

        NavigableMap<LocalDate, BigDecimal> forCurrency = rates.get(currency.toUpperCase());
        Map.Entry<LocalDate, BigDecimal> entry = (forCurrency == null) ? null : forCurrency.floorEntry(date);
        if (entry == null) {
            throw new TaxException("No MNB rate published for " + currency + " on or before " + date);
        }
        return entry;
    }

    private String exchange(RSUDTO rsu) {
        return (rsu.getExchange() == null || rsu.getExchange().isBlank())
                ? DEFAULT_EXCHANGE : rsu.getExchange().toUpperCase();
    }

    private String ticker(RSUDTO rsu) {
        return rsu.getShortName().toUpperCase() + "." + this.exchange(rsu);
    }

    private record PriceSeries(NavigableMap<LocalDate, BigDecimal> closes) {

        Map.Entry<LocalDate, BigDecimal> closeOn(LocalDate date) {
            return this.closes.floorEntry(date);
        }
    }
}
