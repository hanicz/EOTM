package eye.on.the.money.service.crypto;

import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.Writer;
import java.util.List;

public interface TransactionService {

    public List<TransactionDTO> getTransactionsByUserId(String userEmail);
    public void deleteTransactionById(String userEmail, List<Long> ids);
    public List<TransactionDTO> getAllPositions(String userEmail);
    public List<TransactionDTO> getCurrentHoldings(String userEmail, TransactionQuery query);
    public void getCSV(String userEmail, Writer writer);
    public TransactionDTO createTransaction(TransactionDTO transactionDTO, String userEmail);
    public TransactionDTO updateTransaction(TransactionDTO transactionDTO, String userEmail);
    public void processCSV(String userEmail, MultipartFile file);
}
