package eye.on.the.money.service.forex;

import eye.on.the.money.dto.out.ForexTransactionDTO;
import eye.on.the.money.model.User;

import java.util.List;

public interface ForexTransactionService {
    public List<ForexTransactionDTO> getForexTransactionsByUserId(Long userId);
    public void deleteForexTransactionById(User user, List<Long> ids);
    public ForexTransactionDTO createForexTransaction(ForexTransactionDTO forexTransactionDTO, User user);
    public ForexTransactionDTO updateForexTransaction(ForexTransactionDTO forexTransactionDTO, User user);
    public List<ForexTransactionDTO> getAllForexHoldings(Long userId);
}
