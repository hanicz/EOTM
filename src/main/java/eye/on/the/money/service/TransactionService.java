package eye.on.the.money.service;

import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.TransactionDTO;

import java.util.List;

public interface TransactionService {

    public List<TransactionDTO> getTransactionsByUserId(Long userId);
    public List<TransactionDTO> getTransactionsByUserIdWConvCurr(Long userId, String currency);
    public void deleteTransactionById(Long id);
    public List<TransactionDTO> getAllPositions(Long userId, TransactionQuery query);
    public List<TransactionDTO> getCurrentHoldings(Long userId, TransactionQuery query);
}
