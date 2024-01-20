package eye.on.the.money.service.forex;

import eye.on.the.money.dto.out.ForexTransactionDTO;

import java.util.List;

public interface ForexTransactionService {
    public List<ForexTransactionDTO> getForexTransactionsByUserId(String userEmail);

    public void deleteForexTransactionById(String userEmail, List<Long> ids);

    public ForexTransactionDTO createForexTransaction(ForexTransactionDTO forexTransactionDTO, String userEmail);

    public ForexTransactionDTO updateForexTransaction(ForexTransactionDTO forexTransactionDTO, String userEmail);

    public List<ForexTransactionDTO> getAllForexHoldings(String userEmail);
}
