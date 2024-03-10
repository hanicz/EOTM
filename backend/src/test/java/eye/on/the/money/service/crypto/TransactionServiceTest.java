package eye.on.the.money.service.crypto;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.crypto.Transaction;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.repository.crypto.TransactionRepository;
import eye.on.the.money.service.api.CryptoAPIService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
class TransactionServiceTest {

    @MockBean
    private CryptoAPIService cryptoAPIService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private UserRepository userRepository;
    private User user;
    private final ModelMapper modelMapper = new ModelMapper();
    double epsilon = 0.000001d;

    @BeforeEach
    public void init() {
        this.user = this.userRepository.findByEmail("test@test.test");
    }

    @Test
    public void getTransactionsByUserId() {
        List<TransactionDTO> result = this.transactionService.getTransactionsByUserId(this.user.getUsername());
        List<Transaction> transactions = this.transactionRepository.findByUserEmailOrderByTransactionDateDesc(this.user.getUsername());

        Assertions.assertIterableEquals(transactions.stream()
                .map(this::convertToTransactionDTO).collect(Collectors.toList()), result);
    }

    @Test
    public void getAllPositions() {
        List<TransactionDTO> result = this.transactionService.getAllPositions(this.user.getUsername());
        TransactionDTO testObject = result.stream().filter(tDTO -> "DOT".equals(tDTO.getSymbol())).findAny().get();

        Assertions.assertAll("Assert all merged values",
                () -> assertEquals("B", testObject.getBuySell()),
                () -> assertEquals(0.98, testObject.getQuantity(), this.epsilon),
                () -> assertEquals(-43.64, testObject.getAmount(), this.epsilon));
    }

    @Test
    public void getAllPositions2() {
        List<TransactionDTO> result = this.transactionService.getAllPositions(this.user.getUsername());
        TransactionDTO testObject = result.stream().filter(tDTO -> "BTC".equals(tDTO.getSymbol())).findAny().get();

        Assertions.assertAll("Assert all merged values",
                () -> assertEquals("B", testObject.getBuySell()),
                () -> assertEquals(87.4, testObject.getQuantity(), this.epsilon),
                () -> assertEquals(89.13, testObject.getAmount(), this.epsilon));
    }

    @Test
    public void getAllPositions3() {
        List<TransactionDTO> result = this.transactionService.getAllPositions(this.user.getUsername());
        TransactionDTO testObject = result.stream().filter(tDTO -> "ADA".equals(tDTO.getSymbol())).findAny().get();

        Assertions.assertAll("Assert all merged values",
                () -> assertEquals("B", testObject.getBuySell()),
                () -> assertEquals(0.0, testObject.getQuantity()),
                () -> assertEquals(-1031.24, testObject.getAmount(), this.epsilon));
    }

    private TransactionDTO convertToTransactionDTO(Transaction transaction) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(transaction, TransactionDTO.class);
    }
}