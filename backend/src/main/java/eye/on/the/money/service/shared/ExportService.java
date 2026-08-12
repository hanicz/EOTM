package eye.on.the.money.service.shared;

import eye.on.the.money.dto.out.ExportDTO;
import eye.on.the.money.repository.watchlist.CryptoWatchRepository;
import eye.on.the.money.repository.watchlist.ForexWatchRepository;
import eye.on.the.money.repository.watchlist.StockWatchRepository;
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

    private static final int SCHEMA_VERSION = 1;

    private final AccountService accountService;
    private final InvestmentService investmentService;
    private final DividendService dividendService;
    private final ETFInvestmentService etfInvestmentService;
    private final ETFDividendService etfDividendService;
    private final TransactionService transactionService;
    private final ForexTransactionService forexTransactionService;
    private final SecurityTransactionService securityTransactionService;
    private final InterestService interestService;
    private final StockWatchRepository stockWatchRepository;
    private final CryptoWatchRepository cryptoWatchRepository;
    private final ForexWatchRepository forexWatchRepository;
    private final AlertService alertService;
    private final RedditService redditService;

    @Transactional(readOnly = true)
    public ExportDTO export(String userEmail) {
        log.trace("Enter");
        return ExportDTO.builder()
                .schemaVersion(SCHEMA_VERSION)
                .exportedAt(Instant.now())
                .email(userEmail)
                .accounts(this.accounts(userEmail))
                .stock(new ExportDTO.StockSection(
                        this.investmentService.getInvestments(userEmail),
                        this.dividendService.getDividends(userEmail)))
                .etf(new ExportDTO.EtfSection(
                        this.etfInvestmentService.getETFInvestments(userEmail),
                        this.etfDividendService.getDividends(userEmail)))
                .crypto(new ExportDTO.CryptoSection(
                        this.transactionService.getTransactionsByUserId(userEmail)))
                .forex(new ExportDTO.ForexSection(
                        this.forexTransactionService.getForexTransactionsByUserId(userEmail)))
                .securities(new ExportDTO.SecuritiesSection(
                        this.securityTransactionService.getTransactions(userEmail),
                        this.interestService.getInterest(userEmail)))
                .watchlists(this.watchlists(userEmail))
                .alerts(new ExportDTO.AlertSection(
                        this.alertService.getAllStockAlerts(userEmail),
                        this.alertService.getAllCryptoAlerts(userEmail)))
                .preferences(this.preferences(userEmail))
                .build();
    }

    private List<ExportDTO.AccountRow> accounts(String userEmail) {
        return this.accountService.getAccountsByUserEmail(userEmail).stream()
                .map(account -> new ExportDTO.AccountRow(
                        account.getId(), account.getAccountName(), account.getCreationDate()))
                .toList();
    }

    private ExportDTO.WatchlistSection watchlists(String userEmail) {
        List<ExportDTO.StockWatchRow> stock =
                this.stockWatchRepository.findByUserEmailOrderByStockShortName(userEmail).stream()
                        .map(watch -> new ExportDTO.StockWatchRow(watch.getId(), watch.getStock().getShortName(),
                                watch.getStock().getExchange(), watch.getStock().getName()))
                        .toList();

        List<ExportDTO.CryptoWatchRow> crypto =
                this.cryptoWatchRepository.findByUserEmailOrderByCoin_Symbol(userEmail).stream()
                        .map(watch -> new ExportDTO.CryptoWatchRow(watch.getId(), watch.getCoin().getId(),
                                watch.getCoin().getSymbol(), watch.getCoin().getName()))
                        .toList();

        List<ExportDTO.ForexWatchRow> forex =
                this.forexWatchRepository.findByUserEmailOrderByFromCurrencyAscToCurrencyAsc(userEmail).stream()
                        .map(watch -> new ExportDTO.ForexWatchRow(watch.getId(),
                                watch.getFromCurrency().getId(), watch.getToCurrency().getId()))
                        .toList();

        return new ExportDTO.WatchlistSection(stock, crypto, forex);
    }

    private ExportDTO.PreferencesSection preferences(String userEmail) {
        return new ExportDTO.PreferencesSection(
                this.redditService.getSubredditsByUser(userEmail).stream()
                        .map(subreddit -> new ExportDTO.SubredditRow(
                                subreddit.getId(), subreddit.getSubreddit(), subreddit.getDescription()))
                        .toList());
    }
}
