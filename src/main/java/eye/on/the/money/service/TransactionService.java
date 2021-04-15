package eye.on.the.money.service;

import eye.on.the.money.dto.TransactionDTO;

import java.util.List;

public interface TransactionService {

    public List<TransactionDTO> getTransactionsByUserId(Long userId);
    public void deleteTransactionById(Long id);
}
