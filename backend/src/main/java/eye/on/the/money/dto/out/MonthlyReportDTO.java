package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class MonthlyReportDTO {

    private int year;
    private int month;
    private String currency;

    private NetWorthDTO netWorth;
    private ActivitySection activity;
    private List<MonthlyCashFlowDTO> cashFlow;

    public record ActivitySection(List<InvestmentDTO> stockTrades,
                                  List<ETFInvestmentDTO> etfTrades,
                                  List<TransactionDTO> cryptoTrades,
                                  List<ForexTransactionDTO> forexTrades,
                                  List<SecurityTransactionDTO> securityTrades,
                                  List<DividendDTO> dividends,
                                  List<ETFDividendDTO> etfDividends,
                                  List<InterestDTO> interest,
                                  List<AmountRow> dividendTotals,
                                  List<AmountRow> interestTotals) {

        public boolean isEmpty() {
            return this.stockTrades.isEmpty() && this.etfTrades.isEmpty() && this.cryptoTrades.isEmpty()
                    && this.forexTrades.isEmpty() && this.securityTrades.isEmpty()
                    && this.dividends.isEmpty() && this.etfDividends.isEmpty() && this.interest.isEmpty();
        }

        public int tradeCount() {
            return this.stockTrades.size() + this.etfTrades.size() + this.cryptoTrades.size()
                    + this.forexTrades.size() + this.securityTrades.size();
        }
    }

    public record AmountRow(String currencyId, Double amount) {
    }
}
