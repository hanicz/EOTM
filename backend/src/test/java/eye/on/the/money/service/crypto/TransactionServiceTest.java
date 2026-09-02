package eye.on.the.money.service.crypto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.exception.CSVException;
import eye.on.the.money.model.User;
import eye.on.the.money.model.crypto.Transaction;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.repository.crypto.TransactionRepository;
import eye.on.the.money.service.api.CryptoAPIService;
import eye.on.the.money.service.user.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
class TransactionServiceTest {

    @MockitoBean
    private CryptoAPIService cryptoAPIService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private UserRepository userRepository;
    @MockitoBean
    private UserService userService;
    @Autowired
    private ObjectMapper objectMapper;
    private User user;
    private final ModelMapper modelMapper = new ModelMapper();
    double epsilon = 0.000001d;

    @BeforeEach
    public void init() {
        this.user = this.userRepository.findByEmail("test@test.test");
        when(this.userService.getReference(this.user.getId())).thenReturn(this.user);
    }

    @Test
    public void getTransactionsByUserId() {
        List<TransactionDTO> result = this.transactionService.getTransactionsByUserId(this.user.getId());
        List<Transaction> transactions = this.transactionRepository.findByUserIdOrderByTransactionDateDesc(this.user.getId());

        Assertions.assertIterableEquals(transactions.stream()
                .map(this::convertToTransactionDTO).collect(Collectors.toList()), result);
    }

    @Test
    public void getAllPositions() {
        List<TransactionDTO> result = this.transactionService.getAllPositions(this.user.getId());
        TransactionDTO testObject = result.stream().filter(tDTO -> "DOT".equals(tDTO.getSymbol())).findAny().get();

        Assertions.assertAll("Assert all merged values",
                () -> assertEquals("B", testObject.getBuySell()),
                () -> assertEquals(0.98, testObject.getQuantity(), this.epsilon),
                () -> assertEquals(-43.64, testObject.getAmount(), this.epsilon));
    }

    @Test
    public void getAllPositions2() {
        List<TransactionDTO> result = this.transactionService.getAllPositions(this.user.getId());
        TransactionDTO testObject = result.stream().filter(tDTO -> "BTC".equals(tDTO.getSymbol())).findAny().get();

        Assertions.assertAll("Assert all merged values",
                () -> assertEquals("B", testObject.getBuySell()),
                () -> assertEquals(87.4, testObject.getQuantity(), this.epsilon),
                () -> assertEquals(89.13, testObject.getAmount(), this.epsilon));
    }

    @Test
    public void getAllPositions3() {
        List<TransactionDTO> result = this.transactionService.getAllPositions(this.user.getId());
        TransactionDTO testObject = result.stream().filter(tDTO -> "ADA".equals(tDTO.getSymbol())).findAny().get();

        Assertions.assertAll("Assert all merged values",
                () -> assertEquals("B", testObject.getBuySell()),
                () -> assertEquals(0.0, testObject.getQuantity()),
                () -> assertEquals(-1031.24, testObject.getAmount(), this.epsilon));
    }

    @Test
    @Transactional
    public void getAllPositionsReopenedLotIsNotMergedWithClosedLot() {
        this.transactionService.createTransaction(TransactionDTO.builder()
                .buySell("B").quantity(20.0).amount(300.0).currencyId("EUR").fee(0.0)
                .symbol("ADA").transactionString("ttt")
                .transactionDate(LocalDate.of(2021, 6, 1)).build(), this.user.getId());

        List<TransactionDTO> ada = this.transactionService.getAllPositions(this.user.getId()).stream()
                .filter(tDTO -> "ADA".equals(tDTO.getSymbol())).toList();

        TransactionDTO closedLot = ada.stream().filter(t -> t.getQuantity() == 0).findFirst().orElseThrow();
        TransactionDTO openLot = ada.stream().filter(t -> t.getQuantity() > 0).findFirst().orElseThrow();

        Assertions.assertAll("The realised gain stays on the closed lot",
                () -> assertEquals(2, ada.size()),
                () -> assertEquals(-1031.24, closedLot.getAmount(), this.epsilon),
                () -> assertEquals(20.0, openLot.getQuantity(), this.epsilon),
                () -> assertEquals(300.0, openLot.getAmount(), this.epsilon));
    }

    @Test
    public void getCurrentHoldingsWithoutTransactions() {
        List<TransactionDTO> result = this.transactionService.getCurrentHoldings(-1L,
                TransactionQuery.builder().currency("EUR").build());

        assertTrue(result.isEmpty());
        verifyNoInteractions(this.cryptoAPIService);
    }

    @Test
    public void refreshCurrentHoldingsMatchesGetCurrentHoldings() throws JsonProcessingException {
        when(this.cryptoAPIService.getLiveValueForCoins(anyString(), anyString())).thenReturn(this.getCryptoApiResponse());
        TransactionQuery query = TransactionQuery.builder().currency("EUR").build();

        List<TransactionDTO> cached = this.transactionService.getCurrentHoldings(this.user.getId(), query);
        List<TransactionDTO> refreshed = this.transactionService.refreshCurrentHoldings(this.user.getId(), query);

        Assertions.assertIterableEquals(cached, refreshed);
    }

    @Test
    public void getCurrentHoldings() throws JsonProcessingException {
        when(this.cryptoAPIService.getLiveValueForCoins(anyString(), anyString())).thenReturn(this.getCryptoApiResponse());
        List<TransactionDTO> result = this.transactionService.getCurrentHoldings(this.user.getId(), TransactionQuery.builder().currency("EUR").build());

        TransactionDTO testObject = result.stream().filter(tDTO -> "BTC".equals(tDTO.getSymbol())).findAny().get();

        Assertions.assertAll("Assert all merged values",
                () -> assertEquals("B", testObject.getBuySell()),
                () -> assertEquals(87.4, testObject.getQuantity()),
                () -> assertEquals(89.13, testObject.getAmount(), this.epsilon),
                () -> assertEquals(5496673.4, testObject.getLiveValue(), this.epsilon),
                () -> assertEquals(5496584.27, testObject.getValueDiff(), this.epsilon)
        );
    }

    @Test
    public void getCurrentHoldings2() throws JsonProcessingException {
        when(this.cryptoAPIService.getLiveValueForCoins(anyString(), anyString())).thenReturn(this.getCryptoApiResponse());
        List<TransactionDTO> result = this.transactionService.getCurrentHoldings(this.user.getId(), TransactionQuery.builder().currency("EUR").build());

        TransactionDTO testObject = result.stream().filter(tDTO -> "DOT".equals(tDTO.getSymbol())).findAny().get();

        Assertions.assertAll("Assert all merged values",
                () -> assertEquals("B", testObject.getBuySell()),
                () -> assertEquals(0.98, testObject.getQuantity(), this.epsilon),
                () -> assertEquals(-43.64, testObject.getAmount(), this.epsilon),
                () -> assertEquals(6.713, testObject.getLiveValue(), this.epsilon),
                () -> assertEquals(50.353, testObject.getValueDiff(), this.epsilon)
        );
    }

    @Test
    public void getCurrentHoldings3() throws JsonProcessingException {
        when(this.cryptoAPIService.getLiveValueForCoins(anyString(), anyString())).thenReturn(this.getCryptoApiResponse());
        List<TransactionDTO> result = this.transactionService.getCurrentHoldings(this.user.getId(), TransactionQuery.builder().currency("EUR").build());

        Assertions.assertTrue(result.stream().filter(tDTO -> "ADA".equals(tDTO.getSymbol())).findAny().isEmpty());
    }

    @Test
    public void createTransaction() {
        TransactionDTO transactionDTO = this.createNewTransaction();

        TransactionDTO result = this.transactionService.createTransaction(transactionDTO, this.user.getId());

        Assertions.assertAll("Assert new transaction values",
                () -> assertEquals(transactionDTO.getBuySell(), result.getBuySell()),
                () -> assertEquals(transactionDTO.getSymbol(), result.getSymbol()),
                () -> assertEquals(transactionDTO.getQuantity(), result.getQuantity()),
                () -> assertEquals(transactionDTO.getAmount(), result.getAmount()),
                () -> assertEquals(transactionDTO.getTransactionString(), result.getTransactionString()),
                () -> assertEquals(transactionDTO.getFee(), result.getFee()),
                () -> assertEquals(transactionDTO.getCurrencyId(), result.getCurrencyId())
        );
    }

    @Test
    public void createTransactionNoCurrency() {
        TransactionDTO transactionDTO = this.createNewTransaction();
        transactionDTO.setCurrencyId("NOT_EXISTS");

        Assertions.assertThrows(NoSuchElementException.class,
                () -> this.transactionService.createTransaction(transactionDTO, this.user.getId()));
    }

    @Test
    public void createTransactionNoCoin() {
        TransactionDTO transactionDTO = this.createNewTransaction();
        transactionDTO.setSymbol("NOT_EXISTS");

        Assertions.assertThrows(NoSuchElementException.class,
                () -> this.transactionService.createTransaction(transactionDTO, this.user.getId()));
    }

    @Test
    public void deleteTransactionById() {
        TransactionDTO transactionDTO = this.createNewTransaction();
        TransactionDTO inserted = this.transactionService.createTransaction(transactionDTO, this.user.getId());

        this.transactionService.deleteTransactionById(this.user.getId(), List.of(inserted.getId()));

        Assertions.assertTrue(this.transactionRepository.findByIdAndUserId(inserted.getId(), this.user.getId()).isEmpty());
    }

    @Test
    public void deleteTransactionByIdNotExists() {
        Assertions.assertDoesNotThrow(() -> this.transactionService.deleteTransactionById(this.user.getId(), List.of(123456789L)));
    }

    @Test
    public void updateTransaction() {
        TransactionDTO transactionDTO = this.createNewTransaction();
        TransactionDTO inserted = this.transactionService.createTransaction(transactionDTO, this.user.getId());
        inserted.setBuySell("B");
        inserted.setQuantity(10.0);
        inserted.setAmount(100.0);
        inserted.setFee(5.0);

        TransactionDTO result = this.transactionService.updateTransaction(inserted, this.user.getId());
        Assertions.assertAll("Assert new transaction values",
                () -> assertEquals("B", result.getBuySell()),
                () -> assertEquals(10.0, result.getQuantity()),
                () -> assertEquals(100.0, result.getAmount()),
                () -> assertEquals(5.0, result.getFee())
        );
    }

    @Test
    public void updateTransactionNoCurrency() {
        TransactionDTO transactionDTO = this.createNewTransaction();
        TransactionDTO inserted = this.transactionService.createTransaction(transactionDTO, this.user.getId());
        inserted.setCurrencyId("NOT_EXISTS");

        Assertions.assertThrows(NoSuchElementException.class,
                () -> this.transactionService.updateTransaction(inserted, this.user.getId()));
    }

    @Test
    public void updateTransactionNoCoin() {
        TransactionDTO transactionDTO = this.createNewTransaction();
        TransactionDTO inserted = this.transactionService.createTransaction(transactionDTO, this.user.getId());
        inserted.setSymbol("NOT_EXISTS");

        Assertions.assertThrows(NoSuchElementException.class,
                () -> this.transactionService.updateTransaction(inserted, this.user.getId()));
    }

    @Test
    public void updateTransactionNoTransaction() {
        TransactionDTO transactionDTO = this.createNewTransaction();
        TransactionDTO inserted = this.transactionService.createTransaction(transactionDTO, this.user.getId());
        inserted.setId(123456789L);

        Assertions.assertThrows(NoSuchElementException.class,
                () -> this.transactionService.updateTransaction(inserted, this.user.getId()));
    }

    @Test
    public void getCSV() {
        Writer writer = new StringWriter();
        this.transactionService.getCSV(this.user.getId(), writer);
        Assertions.assertAll(
                () -> assertTrue(writer.toString().contains("Transaction Id,Quantity,Type,Transaction Date,Symbol,Amount,Currency,Fee")),
                () -> assertTrue(writer.toString().contains("2,98.5,B,2021-05-07,BTC,100.0,EUR,0.0")),
                () -> assertTrue(writer.toString().contains("5,100.23,B,2021-05-07,ADA,1000.87,EUR,0.0")),
                () -> assertTrue(writer.toString().contains("1,4.98,B,2021-05-20,DOT,156.8,EUR,3.2"))
        );
    }

    @Test
    public void getCSV_Empty() {
        Writer writer = new StringWriter();
        this.transactionService.getCSV(-1L, writer);
        Assertions.assertTrue(writer.toString().isEmpty());
    }

    @Test
    public void processCSV_Update() {
        String csvContent = "Transaction Id,Quantity,Type,Transaction Date,Symbol,Amount,Currency,Fee\n7,1000.0,S,2024-05-20,LUNA,2000.0,EUR,5.0";
        MultipartFile mpf = new MockMultipartFile("file", "file.csv", MediaType.TEXT_PLAIN_VALUE, csvContent.getBytes());

        this.transactionService.processCSV(this.user.getId(), mpf);

        Transaction result = this.transactionRepository.findById(7L).get();

        Assertions.assertAll("Assert new transaction values",
                () -> assertEquals("S", result.getBuySell()),
                () -> assertEquals(1000.0, result.getQuantity()),
                () -> assertEquals(2000.0, result.getAmount()),
                () -> assertEquals(5.0, result.getFee()),
                () -> assertEquals("LUNA", result.getCoin().getSymbol())
        );
    }

    @Test
    public void processCSV_Create() {
        String csvContent = "Transaction Id,Quantity,Type,Transaction Date,Symbol,Amount,Currency,Fee\n,399.0,S,2024-05-29,LUNA,199.0,USD,6.0";
        MultipartFile mpf = new MockMultipartFile("file", "file.csv", MediaType.TEXT_PLAIN_VALUE, csvContent.getBytes());

        this.transactionService.processCSV(this.user.getId(), mpf);

        List<Transaction> transactions = this.transactionRepository.findByUserIdOrderByTransactionDateDesc(this.user.getId());

        Optional<Transaction> result = transactions.stream().filter(d -> d.getQuantity() == 399.0 && d.getCoin().getSymbol().equals("LUNA")).findAny();

        Assertions.assertTrue(result.isPresent());
    }

    @Test
    public void processCSV_Exc() {
        String csvContent = "EXCEPTION,1\n3,EXC,333\n64";
        MultipartFile mpf = new MockMultipartFile("file", "file.csv", MediaType.TEXT_PLAIN_VALUE, csvContent.getBytes());
        Assertions.assertThrows(CSVException.class,
                () -> this.transactionService.processCSV(this.user.getId(), mpf));
    }

    @Test
    public void processCSV_Exc2() {
        String csvContent = "Transaction Id,Quantity,Type,Transaction Date,Symbol,Amount,Currency,Fee\n,399.0,S,NOT_DATE,LUNA,199.0,USD,6.0";
        MultipartFile mpf = new MockMultipartFile("file", "file.csv", MediaType.TEXT_PLAIN_VALUE, csvContent.getBytes());
        Assertions.assertThrows(CSVException.class,
                () -> this.transactionService.processCSV(this.user.getId(), mpf));
    }


    private TransactionDTO convertToTransactionDTO(Transaction transaction) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(transaction, TransactionDTO.class);
    }

    private TransactionDTO createNewTransaction() {
        return TransactionDTO.builder()
                .buySell("S")
                .symbol("LUNA")
                .quantity(20.67)
                .amount(2001.32)
                .transactionDate(LocalDate.now())
                .transactionString("tString")
                .fee(7.2)
                .currencyId("USD")
                .build();
    }

    private JsonNode getCryptoApiResponse() throws JsonProcessingException {
        return this.objectMapper.readTree("""
                    {
                        "bitcoin": {
                            "eur": 62891,
                            "eur_24h_change": 0.7667721469105918
                        },
                        "cardano": {
                            "eur": 0.424517,
                            "eur_24h_change": 1.3623331137501116
                        },
                        "ethereum": {
                            "eur": 3540.9,
                            "eur_24h_change": -0.02278557832247678
                        },
                        "polkadot": {
                            "eur": 6.85,
                            "eur_24h_change": 1.4380905417420442
                        },
                        "terra-luna": {
                            "eur": 0.00010822,
                            "eur_24h_change": 4.486787482828416
                        }
                    }
                """);
    }
}