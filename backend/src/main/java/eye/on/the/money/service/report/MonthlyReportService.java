package eye.on.the.money.service.report;

import eye.on.the.money.dto.out.DividendDTO;
import eye.on.the.money.dto.out.ETFDividendDTO;
import eye.on.the.money.dto.out.InterestDTO;
import eye.on.the.money.dto.out.MonthlyReportDTO;
import eye.on.the.money.service.crypto.TransactionService;
import eye.on.the.money.service.etf.ETFDividendService;
import eye.on.the.money.service.etf.ETFInvestmentService;
import eye.on.the.money.service.financial.BankTransactionService;
import eye.on.the.money.service.forex.ForexTransactionService;
import eye.on.the.money.service.security.InterestService;
import eye.on.the.money.service.security.SecurityTransactionService;
import eye.on.the.money.service.shared.NetWorthService;
import eye.on.the.money.service.stock.DividendService;
import eye.on.the.money.service.stock.InvestmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class MonthlyReportService {

    private static final int SCALE = 2;

    private final NetWorthService netWorthService;
    private final BankTransactionService bankTransactionService;
    private final InvestmentService investmentService;
    private final ETFInvestmentService etfInvestmentService;
    private final TransactionService transactionService;
    private final ForexTransactionService forexTransactionService;
    private final SecurityTransactionService securityTransactionService;
    private final DividendService dividendService;
    private final ETFDividendService etfDividendService;
    private final InterestService interestService;

    public MonthlyReportDTO build(Long userId, YearMonth period, String currency) {
        log.trace("Enter");
        LocalDate from = period.atDay(1);
        LocalDate to = period.atEndOfMonth();

        List<DividendDTO> dividends = this.dividendService.getDividendsBetween(userId, from, to);
        List<ETFDividendDTO> etfDividends = this.etfDividendService.getDividendsBetween(userId, from, to);
        List<InterestDTO> interest = this.interestService.getInterestBetween(userId, from, to);

        MonthlyReportDTO.ActivitySection activity = new MonthlyReportDTO.ActivitySection(
                this.investmentService.getInvestmentsBetween(userId, from, to),
                this.etfInvestmentService.getETFInvestmentsBetween(userId, from, to),
                this.transactionService.getTransactionsBetween(userId, from, to),
                this.forexTransactionService.getForexTransactionsBetween(userId, from, to),
                this.securityTransactionService.getTransactionsBetween(userId, from, to),
                dividends,
                etfDividends,
                interest,
                this.dividendTotals(dividends, etfDividends),
                this.interestTotals(interest));

        MonthlyReportDTO report = MonthlyReportDTO.builder()
                .year(period.getYear())
                .month(period.getMonthValue())
                .currency(currency)
                .netWorth(this.netWorthService.getNetWorth(userId, currency, true))
                .activity(activity)
                .cashFlow(this.bankTransactionService.getCashFlowBetween(userId, from, to))
                .build();

        log.trace("Exit");
        return report;
    }

    private List<MonthlyReportDTO.AmountRow> dividendTotals(List<DividendDTO> stock, List<ETFDividendDTO> etf) {
        Map<String, Double> totals = new TreeMap<>();
        this.accumulate(totals, stock, DividendDTO::getCurrencyId, DividendDTO::getAmount);
        this.accumulate(totals, etf, ETFDividendDTO::getCurrencyId, ETFDividendDTO::getAmount);
        return this.rows(totals);
    }

    private List<MonthlyReportDTO.AmountRow> interestTotals(List<InterestDTO> interest) {
        Map<String, Double> totals = new TreeMap<>();
        this.accumulate(totals, interest, InterestDTO::getCurrencyId, InterestDTO::getAmount);
        return this.rows(totals);
    }

    private <T> void accumulate(Map<String, Double> totals, List<T> rows,
                                Function<T, String> currency, Function<T, Double> amount) {
        for (T row : rows) {
            String key = currency.apply(row);
            Double value = amount.apply(row);
            if (key == null || value == null) continue;
            totals.merge(key.toUpperCase(), value, Double::sum);
        }
    }

    private List<MonthlyReportDTO.AmountRow> rows(Map<String, Double> totals) {
        return totals.entrySet().stream()
                .map(entry -> new MonthlyReportDTO.AmountRow(entry.getKey(), this.scaled(entry.getValue())))
                .toList();
    }

    private Double scaled(Double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP).doubleValue();
    }
}
