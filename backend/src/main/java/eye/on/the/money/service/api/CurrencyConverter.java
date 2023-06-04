package eye.on.the.money.service.api;

import eye.on.the.money.dto.out.*;

import java.util.List;

public interface CurrencyConverter {
    public void changeInvestmentsCurrency(List<InvestmentDTO> investments, String toCurrency);
    public void changeLiveValueCurrency(List<InvestmentDTO> investments, String toCurrency);
    public void changeTransactionsCurrency(List<TransactionDTO> transactions, String toCurrency);
    public void changeForexWatchList(List<ForexWatchDTO> forexWatchList);
    public void changeETFCurrency(List<ETFInvestmentDTO> investments, String toCurrency);
    public void changeLiveValueCurrencyForForexTransactions(List<ForexTransactionDTO> forexTransactions);
}
