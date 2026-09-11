package eye.on.the.money.service.news;

import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.exception.ValidationException;
import eye.on.the.money.model.news.News;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.watchlist.TickerWatch;
import eye.on.the.money.repository.watchlist.StockWatchRepository;
import eye.on.the.money.service.api.NewsAPIService;
import eye.on.the.money.service.etf.ETFInvestmentService;
import eye.on.the.money.service.stock.InvestmentService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NewsServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private NewsAPIService newsAPIService;
    @Mock
    private InvestmentService investmentService;
    @Mock
    private ETFInvestmentService etfInvestmentService;
    @Mock
    private StockWatchRepository stockWatchRepository;

    @InjectMocks
    private NewsService newsService;

    @BeforeEach
    void noHoldingsByDefault() {
        when(this.investmentService.getCurrentHoldings(NewsServiceTest.USER_ID)).thenReturn(List.of());
        when(this.etfInvestmentService.getCurrentETFHoldings(NewsServiceTest.USER_ID)).thenReturn(List.of());
        when(this.stockWatchRepository.findByUserIdOrderByStockShortName(NewsServiceTest.USER_ID))
                .thenReturn(List.of());
    }

    @Test
    void mergesHoldingsAndWatchlistTagsAndSortsByDatetimeDescending() {
        when(this.investmentService.getCurrentHoldings(NewsServiceTest.USER_ID))
                .thenReturn(List.of(this.holding("AAAA", "US")));
        when(this.stockWatchRepository.findByUserIdOrderByStockShortName(NewsServiceTest.USER_ID))
                .thenReturn(List.of(this.watch("BBBB", "US")));
        when(this.newsAPIService.getCompanyNews("AAAA"))
                .thenReturn(this.stories(this.story("https://news.example.com/1", 100L)));
        when(this.newsAPIService.getCompanyNews("BBBB"))
                .thenReturn(this.stories(this.story("https://news.example.com/2", 300L)));

        List<News> result = this.newsService.getPortfolioNews(NewsServiceTest.USER_ID);

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("https://news.example.com/2", result.get(0).getUrl());
        Assertions.assertEquals("BBBB", result.get(0).getSymbol());
        Assertions.assertEquals("AAAA", result.get(1).getSymbol());
    }

    @Test
    void dedupesTheSameStoryKeepingTheFirstSymbol() {
        when(this.investmentService.getCurrentHoldings(NewsServiceTest.USER_ID))
                .thenReturn(List.of(this.holding("AAAA", "US"), this.holding("BBBB", "US")));
        when(this.newsAPIService.getCompanyNews("AAAA"))
                .thenReturn(this.stories(this.story("https://news.example.com/shared", 100L)));
        when(this.newsAPIService.getCompanyNews("BBBB"))
                .thenReturn(this.stories(this.story("https://news.example.com/shared", 100L)));

        List<News> result = this.newsService.getPortfolioNews(NewsServiceTest.USER_ID);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("AAAA", result.get(0).getSymbol());
    }

    @Test
    void skipsNonUsExchanges() {
        when(this.investmentService.getCurrentHoldings(NewsServiceTest.USER_ID))
                .thenReturn(List.of(this.holding("CCCC", "LSE")));
        when(this.stockWatchRepository.findByUserIdOrderByStockShortName(NewsServiceTest.USER_ID))
                .thenReturn(List.of(this.watch("DDDD", "XETRA")));

        List<News> result = this.newsService.getPortfolioNews(NewsServiceTest.USER_ID);

        Assertions.assertTrue(result.isEmpty());
        verify(this.newsAPIService, never()).getCompanyNews(anyString());
    }

    @Test
    void oneFailingSymbolStillYieldsTheOthers() {
        when(this.investmentService.getCurrentHoldings(NewsServiceTest.USER_ID))
                .thenReturn(List.of(this.holding("AAAA", "US"), this.holding("BBBB", "US")));
        when(this.newsAPIService.getCompanyNews("AAAA")).thenThrow(new APIException("Finnhub is down"));
        when(this.newsAPIService.getCompanyNews("BBBB"))
                .thenReturn(this.stories(this.story("https://news.example.com/2", 200L)));

        List<News> result = this.newsService.getPortfolioNews(NewsServiceTest.USER_ID);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("BBBB", result.get(0).getSymbol());
    }

    @Test
    void failingHoldingsDegradeToTheWatchlist() {
        when(this.investmentService.getCurrentHoldings(NewsServiceTest.USER_ID))
                .thenThrow(new APIException("Live prices unavailable"));
        when(this.stockWatchRepository.findByUserIdOrderByStockShortName(NewsServiceTest.USER_ID))
                .thenReturn(List.of(this.watch("BBBB", "US")));
        when(this.newsAPIService.getCompanyNews("BBBB"))
                .thenReturn(this.stories(this.story("https://news.example.com/2", 200L)));

        List<News> result = this.newsService.getPortfolioNews(NewsServiceTest.USER_ID);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("BBBB", result.get(0).getSymbol());
    }

    @Test
    void capsTheNumberOfSymbolsFannedOut() {
        List<InvestmentDTO> holdings = new ArrayList<>();
        for (int index = 0; index < 30; index++) {
            holdings.add(this.holding("SYM" + index, "US"));
        }
        when(this.investmentService.getCurrentHoldings(NewsServiceTest.USER_ID)).thenReturn(holdings);
        when(this.newsAPIService.getCompanyNews(anyString())).thenReturn(List.of());

        this.newsService.getPortfolioNews(NewsServiceTest.USER_ID);

        verify(this.newsAPIService, times(15)).getCompanyNews(anyString());
    }

    @Test
    void dedupesATickerThatIsBothHeldAndWatched() {
        when(this.investmentService.getCurrentHoldings(NewsServiceTest.USER_ID))
                .thenReturn(List.of(this.holding("AAAA", "US")));
        when(this.stockWatchRepository.findByUserIdOrderByStockShortName(NewsServiceTest.USER_ID))
                .thenReturn(List.of(this.watch("aaaa", "US")));
        when(this.newsAPIService.getCompanyNews("AAAA")).thenReturn(List.of());

        this.newsService.getPortfolioNews(NewsServiceTest.USER_ID);

        verify(this.newsAPIService, times(1)).getCompanyNews("AAAA");
    }

    @Test
    void includesUsEtfHoldings() {
        when(this.etfInvestmentService.getCurrentETFHoldings(NewsServiceTest.USER_ID))
                .thenReturn(List.of(this.etfHolding("EEEE", "US")));
        when(this.newsAPIService.getCompanyNews("EEEE"))
                .thenReturn(this.stories(this.story("https://news.example.com/3", 400L)));

        List<News> result = this.newsService.getPortfolioNews(NewsServiceTest.USER_ID);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("EEEE", result.get(0).getSymbol());
    }

    @Test
    void leavesOutStoriesWithoutAUrl() {
        when(this.investmentService.getCurrentHoldings(NewsServiceTest.USER_ID))
                .thenReturn(List.of(this.holding("AAAA", "US")));
        when(this.newsAPIService.getCompanyNews("AAAA")).thenReturn(this.stories(this.story(null, 100L)));

        Assertions.assertTrue(this.newsService.getPortfolioNews(NewsServiceTest.USER_ID).isEmpty());
    }

    @Test
    void rejectsAnUnknownCategory() {
        Assertions.assertThrows(ValidationException.class, () -> this.newsService.getCategoryNews("nonsense"));
        verify(this.newsAPIService, never()).getNews(anyString());
    }

    @Test
    void passesAKnownCategoryThroughLowerCased() {
        when(this.newsAPIService.getNews("general"))
                .thenReturn(this.stories(this.story("https://news.example.com/4", 500L)));

        List<News> result = this.newsService.getCategoryNews("General");

        Assertions.assertEquals(1, result.size());
        verify(this.newsAPIService).getNews("general");
    }

    @Test
    void normalisesTheCompanySymbol() {
        when(this.newsAPIService.getCompanyNews("AAAA"))
                .thenReturn(this.stories(this.story("https://news.example.com/5", 600L)));

        List<News> result = this.newsService.getCompanyNews(" aaaa ");

        Assertions.assertEquals(1, result.size());
        verify(this.newsAPIService).getCompanyNews("AAAA");
    }

    private InvestmentDTO holding(String shortName, String exchange) {
        return InvestmentDTO.builder().shortName(shortName).exchange(exchange).quantity(5).build();
    }

    private ETFInvestmentDTO etfHolding(String shortName, String exchange) {
        return ETFInvestmentDTO.builder().shortName(shortName).exchange(exchange).quantity(5).build();
    }

    private TickerWatch watch(String shortName, String exchange) {
        return TickerWatch.builder()
                .stock(Stock.builder().id(shortName + "." + exchange).shortName(shortName).exchange(exchange).build())
                .build();
    }

    private List<News> stories(News... news) {
        return new ArrayList<>(List.of(news));
    }

    private News story(String url, Long datetime) {
        return News.builder().url(url).datetime(datetime).headline("Quarterly results beat estimates")
                .source("Example Wire").summary("A made up summary.").build();
    }
}
