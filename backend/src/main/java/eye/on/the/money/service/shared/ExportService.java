package eye.on.the.money.service.shared;

import eye.on.the.money.dto.out.ExportDTO;
import eye.on.the.money.repository.watchlist.CryptoWatchRepository;
import eye.on.the.money.repository.watchlist.ForexWatchRepository;
import eye.on.the.money.repository.watchlist.StockWatchRepository;
import eye.on.the.money.dto.out.CashDTO;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Collects everything a user owns into a single document.
 * <p>
 * Reads stored data only - nothing here calls an external API, so an export cannot fail because EODHD is
 * down or a subscription lapsed. That is why the watchlists come from their repositories rather than from
 * {@link WatchListService}, whose getters enrich each row with a live price.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExportService {

    private static final int SCHEMA_VERSION = 3;

    private final UserService userService;
    private final AccountService accountService;
    private final InvestmentService investmentService;
    private final DividendService dividendService;
    private final ETFInvestmentService etfInvestmentService;
    private final ETFDividendService etfDividendService;
    private final TransactionService transactionService;
    private final ForexTransactionService forexTransactionService;
    private final SecurityTransactionService securityTransactionService;
    private final InterestService interestService;
    private final CashService cashService;
    private final StockWatchRepository stockWatchRepository;
    private final CryptoWatchRepository cryptoWatchRepository;
    private final ForexWatchRepository forexWatchRepository;
    private final AlertService alertService;
    private final RedditService redditService;

    @Transactional(readOnly = true)
    public ExportDTO export(Long userId) {
        log.trace("Enter");
        String userEmail = this.userService.loadUserById(userId).getEmail();
        return ExportDTO.builder()
                .schemaVersion(SCHEMA_VERSION)
                .exportedAt(Instant.now())
                .email(userEmail)
                .accounts(this.accounts(userId))
                .stock(new ExportDTO.StockSection(
                        this.investmentService.getInvestments(userId),
                        this.dividendService.getDividends(userId)))
                .etf(new ExportDTO.EtfSection(
                        this.etfInvestmentService.getETFInvestments(userId),
                        this.etfDividendService.getDividends(userId)))
                .crypto(new ExportDTO.CryptoSection(
                        this.transactionService.getTransactionsByUserId(userId)))
                .forex(new ExportDTO.ForexSection(
                        this.forexTransactionService.getForexTransactionsByUserId(userId)))
                .securities(new ExportDTO.SecuritiesSection(
                        this.securityTransactionService.getTransactions(userId),
                        this.interestService.getInterest(userId)))
                .cash(this.cash(userId))
                .watchlists(this.watchlists(userId))
                .alerts(new ExportDTO.AlertSection(
                        this.alertService.getAllStockAlerts(userId),
                        this.alertService.getAllCryptoAlerts(userId)))
                .preferences(this.preferences(userId))
                .build();
    }

    private List<ExportDTO.AccountRow> accounts(Long userId) {
        return this.accountService.getAccountsByUserId(userId).stream()
                .map(account -> new ExportDTO.AccountRow(
                        account.getId(), account.getAccountName(), account.getCreationDate()))
                .toList();
    }

    private ExportDTO.CashSection cash(Long userId) {
        CashDTO cash = this.cashService.getCash(userId);
        return new ExportDTO.CashSection(cash.getAmount(), cash.getCurrency());
    }

    private ExportDTO.WatchlistSection watchlists(Long userId) {
        List<ExportDTO.StockWatchRow> stock =
                this.stockWatchRepository.findByUserIdOrderByStockShortName(userId).stream()
                        .map(watch -> new ExportDTO.StockWatchRow(watch.getId(), watch.getStock().getShortName(),
                                watch.getStock().getExchange(), watch.getStock().getName(),
                                watch.getGroup() == null ? null : watch.getGroup().getName()))
                        .toList();

        List<ExportDTO.CryptoWatchRow> crypto =
                this.cryptoWatchRepository.findByUserIdOrderByCoin_Symbol(userId).stream()
                        .map(watch -> new ExportDTO.CryptoWatchRow(watch.getId(), watch.getCoin().getId(),
                                watch.getCoin().getSymbol(), watch.getCoin().getName()))
                        .toList();

        List<ExportDTO.ForexWatchRow> forex =
                this.forexWatchRepository.findByUserIdOrderByFromCurrencyAscToCurrencyAsc(userId).stream()
                        .map(watch -> new ExportDTO.ForexWatchRow(watch.getId(),
                                watch.getFromCurrency().getId(), watch.getToCurrency().getId()))
                        .toList();

        return new ExportDTO.WatchlistSection(stock, crypto, forex);
    }

    private ExportDTO.PreferencesSection preferences(Long userId) {
        return new ExportDTO.PreferencesSection(
                this.redditService.getSubredditsByUser(userId).stream()
                        .map(subreddit -> new ExportDTO.SubredditRow(
                                subreddit.getId(), subreddit.getSubreddit(), subreddit.getDescription()))
                        .toList());
    }
}
