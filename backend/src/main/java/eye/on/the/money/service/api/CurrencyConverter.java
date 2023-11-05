package eye.on.the.money.service.api;

import eye.on.the.money.dto.out.*;

import java.util.List;

public interface CurrencyConverter {
    public void changeTransactionsCurrency(List<TransactionDTO> transactions, String toCurrency);
    public void forexWatchList(List<ForexWatchDTO> forexWatchList);
    public void changeLiveValueCurrencyForForexTransactions(List<ForexTransactionDTO> forexTransactions);
}
