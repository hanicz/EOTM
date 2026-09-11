package eye.on.the.money.service.news;

import eye.on.the.money.exception.APIException;
import eye.on.the.money.exception.ValidationException;
import eye.on.the.money.model.news.News;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.watchlist.TickerWatch;
import eye.on.the.money.repository.watchlist.StockWatchRepository;
import eye.on.the.money.service.api.NewsAPIService;
import eye.on.the.money.service.etf.ETFInvestmentService;
import eye.on.the.money.service.stock.InvestmentService;
import eye.on.the.money.util.Ticker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsService {

    private static final String US_EXCHANGE = "US";
    private static final int MAX_SYMBOLS = 15;
    private static final int MAX_ITEMS = 120;
    private static final Set<String> CATEGORIES = Set.of("general", "forex", "crypto", "merger");

    private final NewsAPIService newsAPIService;
    private final InvestmentService investmentService;
    private final ETFInvestmentService etfInvestmentService;
    private final StockWatchRepository stockWatchRepository;

    @Cacheable(cacheNames = "news-category", key = "#category.toLowerCase()")
    public List<News> getCategoryNews(String category) {
        String normalized = category.trim().toLowerCase(Locale.ROOT);
        if (!CATEGORIES.contains(normalized)) {
            throw new ValidationException("Unknown news category: " + category);
        }
        return this.newsAPIService.getNews(normalized);
    }

    @Cacheable(cacheNames = "news-company", key = "#symbol.toUpperCase()")
    public List<News> getCompanyNews(String symbol) {
        return this.newsAPIService.getCompanyNews(Ticker.normalizeShortName(symbol));
    }

    @Cacheable(cacheNames = "news-portfolio", key = "#userId")
    public List<News> getPortfolioNews(Long userId) {
        List<String> symbols = this.portfolioSymbols(userId);
        if (symbols.isEmpty()) {
            log.info("No US tickers held or watched, portfolio news feed is empty");
            return List.of();
        }
        return this.merge(this.fetchAll(symbols));
    }

    private List<String> portfolioSymbols(Long userId) {
        return Stream.of(this.heldStockSymbols(userId), this.heldEtfSymbols(userId), this.watchedSymbols(userId))
                .flatMap(List::stream)
                .distinct()
                .limit(NewsService.MAX_SYMBOLS)
                .toList();
    }

    private List<String> heldStockSymbols(Long userId) {
        try {
            return this.investmentService.getCurrentHoldings(userId).stream()
                    .filter(investment -> NewsService.US_EXCHANGE.equalsIgnoreCase(investment.getExchange()))
                    .map(investment -> this.normalize(investment.getShortName()))
                    .filter(Objects::nonNull)
                    .toList();
        } catch (APIException e) {
            log.warn("Unable to load stock holdings for the portfolio news feed", e);
            return List.of();
        }
    }

    private List<String> heldEtfSymbols(Long userId) {
        try {
            return this.etfInvestmentService.getCurrentETFHoldings(userId).stream()
                    .filter(investment -> NewsService.US_EXCHANGE.equalsIgnoreCase(investment.getExchange()))
                    .map(investment -> this.normalize(investment.getShortName()))
                    .filter(Objects::nonNull)
                    .toList();
        } catch (APIException e) {
            log.warn("Unable to load ETF holdings for the portfolio news feed", e);
            return List.of();
        }
    }

    private List<String> watchedSymbols(Long userId) {
        return this.stockWatchRepository.findByUserIdOrderByStockShortName(userId).stream()
                .map(TickerWatch::getStock)
                .filter(Objects::nonNull)
                .filter(stock -> NewsService.US_EXCHANGE.equalsIgnoreCase(stock.getExchange()))
                .map(Stock::getShortName)
                .map(this::normalize)
                .filter(Objects::nonNull)
                .toList();
    }

    private String normalize(String shortName) {
        if (shortName == null || shortName.isBlank()) {
            return null;
        }
        return shortName.trim().toUpperCase(Locale.ROOT);
    }

    private List<News> fetchAll(List<String> symbols) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<List<News>>> futures = symbols.stream()
                    .map(symbol -> CompletableFuture
                            .supplyAsync(() -> this.newsAPIService.getCompanyNews(symbol), executor)
                            .handle((news, throwable) -> this.tagged(symbol, news, throwable)))
                    .toList();
            return futures.stream().map(CompletableFuture::join).flatMap(List::stream).toList();
        }
    }

    private List<News> tagged(String symbol, List<News> news, Throwable throwable) {
        if (throwable != null) {
            log.warn("Unable to load news for {}, leaving it out of the feed", symbol, throwable);
            return List.of();
        }
        news.forEach(item -> item.setSymbol(symbol));
        return news;
    }

    private List<News> merge(List<News> items) {
        Map<String, News> unique = new LinkedHashMap<>();
        for (News item : items) {
            if (item.getUrl() != null && !item.getUrl().isBlank()) {
                unique.putIfAbsent(item.getUrl(), item);
            }
        }
        return unique.values().stream()
                .sorted(Comparator.comparing(News::getDatetime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(NewsService.MAX_ITEMS)
                .toList();
    }
}
