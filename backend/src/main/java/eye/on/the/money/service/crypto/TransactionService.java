package eye.on.the.money.service.crypto;

import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.Writer;
import java.util.List;

public interface TransactionService {

    public List<TransactionDTO> getTransactionsByUserId(Long userId);
    public void deleteTransactionById(User user, List<Long> ids);
    public List<TransactionDTO> getAllPositions(Long userId);
    public List<TransactionDTO> getCurrentHoldings(Long userId, TransactionQuery query);
    public void getCSV(Long userId, Writer writer);
    public TransactionDTO createTransaction(TransactionDTO transactionDTO, User user);
    public TransactionDTO updateTransaction(TransactionDTO transactionDTO, User user);
    public void processCSV(User user, MultipartFile file);
}
