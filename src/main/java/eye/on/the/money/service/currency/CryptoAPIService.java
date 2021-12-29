package eye.on.the.money.service.currency;

import eye.on.the.money.dto.out.TransactionDTO;

import java.util.List;

public interface CryptoAPIService {
    public void getLiveValue(List<TransactionDTO> transactionDTOList, String currency);
}
