package eye.on.the.money.service.shared;

import eye.on.the.money.dto.out.CashDTO;
import eye.on.the.money.dto.out.DividendDTO;
import eye.on.the.money.dto.out.ExportDTO;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.dto.out.StockAlertDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.model.reddit.Subreddit;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Account;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.watchlist.CryptoWatch;
import eye.on.the.money.model.watchlist.ForexWatch;
import eye.on.the.money.model.watchlist.TickerWatch;
import eye.on.the.money.repository.watchlist.CryptoWatchRepository;
import eye.on.the.money.repository.watchlist.ForexWatchRepository;
import eye.on.the.money.repository.watchlist.StockWatchRepository;
import eye.on.the.money.service.cash.CashService;
import eye.on.the.money.service.crypto.TransactionService;
import eye.on.the.money.service.etf.ETFDividendService;
import eye.on.the.money.service.etf.ETFInvestmentService;
import eye.on.the.money.service.forex.ForexTransactionService;
import eye.on.the.money.service.reddit.RedditService;
import eye.on.the.money.service.security.InterestService;
import eye.on.the.money.service.security.SecurityTransactionService;
import eye.on.the.money.service.stock.AccountService;
import eye.on.the.money.service.stock.DividendService;
import eye.on.the.money.service.stock.InvestmentService;
import eye.on.the.money.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExportServiceTest {

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "user@example.com";

    @Mock private UserService userService;
    @Mock private AccountService accountService;
    @Mock private InvestmentService investmentService;
    @Mock private DividendService dividendService;
    @Mock private ETFInvestmentService etfInvestmentService;
    @Mock private ETFDividendService etfDividendService;
    @Mock private TransactionService transactionService;
    @Mock private ForexTransactionService forexTransactionService;
    @Mock private SecurityTransactionService securityTransactionService;
    @Mock private InterestService interestService;
    @Mock private CashService cashService;
    @Mock private StockWatchRepository stockWatchRepository;
    @Mock private CryptoWatchRepository cryptoWatchRepository;
    @Mock private ForexWatchRepository forexWatchRepository;
    @Mock private AlertService alertService;
    @Mock private RedditService redditService;

    @InjectMocks
    private ExportService exportService;

    @BeforeEach
    void stubTheOwner() {
        when(this.userService.loadUserById(USER_ID))
                .thenReturn(User.builder().id(USER_ID).email(USER_EMAIL).build());
        when(this.cashService.getCash(USER_ID))
                .thenReturn(CashDTO.builder().amount(0.0).currency("HUF").build());
    }

    @Test
    void export_gathersEveryUserOwnedCollection() {
        when(this.accountService.getAccountsByUserId(USER_ID)).thenReturn(List.of(
                Account.builder().id(1L).accountName("Main").creationDate(LocalDate.of(2021, 1, 4)).build()));
        when(this.investmentService.getInvestments(USER_ID)).thenReturn(List.of(
                InvestmentDTO.builder().investmentId(12L).shortName("AAPL").build()));
        when(this.dividendService.getDividends(USER_ID)).thenReturn(List.of(
                DividendDTO.builder().dividendId(3L).build()));
        when(this.alertService.getAllStockAlerts(USER_ID)).thenReturn(List.of(
                StockAlertDTO.builder().id(7L).shortName("AAPL").build()));
        when(this.redditService.getSubredditsByUser(USER_ID)).thenReturn(List.of(
                Subreddit.builder().id(2L).subreddit("investing").description("DD").build()));

        ExportDTO export = this.exportService.export(USER_ID);

        assertEquals(3, export.getSchemaVersion());
        assertEquals(USER_EMAIL, export.getEmail());
        assertNotNull(export.getExportedAt());
        assertEquals("Main", export.getAccounts().getFirst().accountName());
        assertEquals(12L, export.getStock().investments().getFirst().getInvestmentId());
        assertEquals(3L, export.getStock().dividends().getFirst().getDividendId());
        assertEquals(7L, export.getAlerts().stock().getFirst().getId());
        assertEquals("investing", export.getPreferences().subreddits().getFirst().subreddit());
    }

    @Test
    void export_flattensWatchlistsWithoutLivePrices() {
        when(this.stockWatchRepository.findByUserIdOrderByStockShortName(anyLong())).thenReturn(List.of(
                TickerWatch.builder().id(1L)
                        .stock(Stock.builder().shortName("AAPL").exchange("US").name("Apple Inc.").build()).build()));
        when(this.cryptoWatchRepository.findByUserIdOrderByCoin_Symbol(anyLong())).thenReturn(List.of(
                CryptoWatch.builder().id(2L)
                        .coin(Coin.builder().id("bitcoin").symbol("btc").name("Bitcoin").build()).build()));
        when(this.forexWatchRepository.findByUserIdOrderByFromCurrencyAscToCurrencyAsc(anyLong())).thenReturn(
                List.of(ForexWatch.builder().id(3L)
                        .fromCurrency(new Currency("EUR", "Euro"))
                        .toCurrency(new Currency("HUF", "Forint")).build()));

        ExportDTO.WatchlistSection watchlists = this.exportService.export(USER_ID).getWatchlists();

        assertEquals(new ExportDTO.StockWatchRow(1L, "AAPL", "US", "Apple Inc.", null), watchlists.stock().getFirst());
        assertEquals(new ExportDTO.CryptoWatchRow(2L, "bitcoin", "btc", "Bitcoin"), watchlists.crypto().getFirst());
        assertEquals(new ExportDTO.ForexWatchRow(3L, "EUR", "HUF"), watchlists.forex().getFirst());
    }

    @Test
    void export_includesTheCashBalance() {
        when(this.cashService.getCash(USER_ID))
                .thenReturn(CashDTO.builder().amount(1250000.0).currency("HUF").build());

        ExportDTO export = this.exportService.export(USER_ID);

        assertEquals(new ExportDTO.CashSection(1250000.0, "HUF"), export.getCash());
    }

    @Test
    void export_returnsEmptySectionsForAFreshAccount() {
        ExportDTO export = this.exportService.export(USER_ID);

        assertEquals(0, export.getAccounts().size());
        assertEquals(0, export.getStock().investments().size());
        assertEquals(0, export.getWatchlists().crypto().size());
        assertEquals(0, export.getPreferences().subreddits().size());
    }
}
