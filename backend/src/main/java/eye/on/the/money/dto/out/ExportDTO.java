package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A user's whole account in one document, for taking a backup of their own data.
 * <p>
 * Deliberately left out: the password hash, the deployment's API credentials, the shared reference tables
 * (stocks, coins, securities, exchanges, currencies) which are re-fetchable and would dwarf the account,
 * and any live or derived value, which would be stale and misleading in a file dated months ago.
 */
@Slf4j
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class ExportDTO {

    /** Bumped whenever the shape changes, so a future import can tell what it is reading. */
    private int schemaVersion;
    private Instant exportedAt;
    private String email;

    private List<AccountRow> accounts;
    private StockSection stock;
    private EtfSection etf;
    private CryptoSection crypto;
    private ForexSection forex;
    private SecuritiesSection securities;
    private CashSection cash;
    private NoteSection note;
    private WatchlistSection watchlists;
    private AlertSection alerts;
    private PreferencesSection preferences;

    public record AccountRow(Long id, String accountName, LocalDate creationDate) {
    }

    public record StockSection(List<InvestmentDTO> investments, List<DividendDTO> dividends) {
    }

    public record EtfSection(List<ETFInvestmentDTO> investments, List<ETFDividendDTO> dividends) {
    }

    public record CryptoSection(List<TransactionDTO> transactions) {
    }

    public record ForexSection(List<ForexTransactionDTO> transactions) {
    }

    public record SecuritiesSection(List<SecurityTransactionDTO> transactions, List<InterestDTO> interest) {
    }

    public record CashSection(Double amount, String currency) {
    }

    public record NoteSection(String content, LocalDateTime updatedAt) {
    }

    public record WatchlistSection(List<StockWatchRow> stock, List<CryptoWatchRow> crypto,
                                   List<ForexWatchRow> forex) {
    }

    /** Watchlist rows are kept flat and price-free; the live values the UI shows are not worth backing up. */
    public record StockWatchRow(Long id, String shortName, String exchange, String name, String groupName) {
    }

    public record CryptoWatchRow(Long id, String coinId, String symbol, String name) {
    }

    public record ForexWatchRow(Long id, String fromCurrencyId, String toCurrencyId) {
    }

    public record AlertSection(List<StockAlertDTO> stock, List<CryptoAlertDTO> crypto) {
    }

    public record PreferencesSection(List<SubredditRow> subreddits) {
    }

    public record SubredditRow(Long id, String subreddit, String description) {
    }
}
