package eye.on.the.money.service.forex;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.ForexTransactionDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.forex.ForexTransaction;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.forex.ForexTransactionRepository;
import eye.on.the.money.service.api.EODAPIService;
import eye.on.the.money.service.user.UserServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.mockito.Mockito.when;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class ForexTransactionServiceTest {

    @Autowired
    private ForexTransactionService forexTransactionService;
    @Autowired
    private CurrencyRepository currencyRepository;
    @Autowired
    private ForexTransactionRepository forexTransactionRepository;
    @MockitoBean
    private EODAPIService eodapiService;
    @MockitoBean
    private UserServiceImpl userService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    public void init() {
        this.user = this.userRepository.findByEmail("test@test.test");
        when(this.userService.loadUserByEmail(this.user.getUsername())).thenReturn(this.user);
    }


    @Test
    public void getForexTransactionsByUserId() {
        List<ForexTransactionDTO> result = this.forexTransactionService.getForexTransactionsByUserId(this.user.getUsername());
        List<ForexTransaction> actual = this.forexTransactionRepository.findByUserEmailOrderByTransactionDate(this.user.getUsername());

        Assertions.assertIterableEquals(actual.stream().map(this::convertToForexTransactionDTO).collect(Collectors.toList()), result);
    }

    @Test
    public void createForexTransactionBuy() {
        ForexTransactionDTO ftDTO = this.getFTDTO();

        ForexTransactionDTO result = this.forexTransactionService.createForexTransaction(ftDTO, this.user.getUsername());

        Assertions.assertAll("Assert all values",
                () -> Assertions.assertNotNull(result.getForexTransactionId()),
                () -> Assertions.assertEquals(ftDTO.getFromAmount(), result.getFromAmount()),
                () -> Assertions.assertEquals(ftDTO.getToAmount(), result.getToAmount()),
                () -> Assertions.assertEquals(ftDTO.getFromCurrencyId(), result.getFromCurrencyId()),
                () -> Assertions.assertEquals(ftDTO.getToCurrencyId(), result.getToCurrencyId()),
                () -> Assertions.assertEquals(ftDTO.getBuySell(), result.getBuySell()),
                () -> Assertions.assertEquals(ftDTO.getTransactionDate(), result.getTransactionDate()),
                () -> Assertions.assertEquals(ftDTO.getFromAmount() / ftDTO.getToAmount(), result.getChangeRate())
        );
    }

    @Test
    public void createForexTransactionSell() {
        ForexTransactionDTO ftDTO = this.getFTDTO();
        ftDTO.setBuySell("S");

        ForexTransactionDTO result = this.forexTransactionService.createForexTransaction(ftDTO, this.user.getUsername());

        Assertions.assertAll("Assert all values",
                () -> Assertions.assertNotNull(result.getForexTransactionId()),
                () -> Assertions.assertEquals(ftDTO.getFromAmount(), result.getFromAmount()),
                () -> Assertions.assertEquals(ftDTO.getToAmount(), result.getToAmount()),
                () -> Assertions.assertEquals(ftDTO.getFromCurrencyId(), result.getFromCurrencyId()),
                () -> Assertions.assertEquals(ftDTO.getToCurrencyId(), result.getToCurrencyId()),
                () -> Assertions.assertEquals(ftDTO.getBuySell(), result.getBuySell()),
                () -> Assertions.assertEquals(ftDTO.getTransactionDate(), result.getTransactionDate()),
                () -> Assertions.assertEquals(ftDTO.getToAmount() / ftDTO.getFromAmount(), result.getChangeRate())
        );
    }

    @Test
    public void createForexTransactionNoFromCurrency() {
        ForexTransactionDTO ftDTO = this.getFTDTO();
        ftDTO.setFromCurrencyId("NOT_EXISTS");

        Assertions.assertThrows(NoSuchElementException.class,
                () -> this.forexTransactionService.createForexTransaction(ftDTO, this.user.getUsername()));
    }

    @Test
    public void createForexTransactionNoToCurrency() {
        ForexTransactionDTO ftDTO = this.getFTDTO();
        ftDTO.setToCurrencyId("NOT_EXISTS");

        Assertions.assertThrows(NoSuchElementException.class,
                () -> this.forexTransactionService.createForexTransaction(ftDTO, this.user.getUsername()));
    }

    private ForexTransactionDTO convertToForexTransactionDTO(ForexTransaction transaction) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(transaction, ForexTransactionDTO.class);
    }

    private ForexTransactionDTO getFTDTO() {
        return ForexTransactionDTO.builder()
                .buySell("B")
                .fromAmount(120.0)
                .toAmount(100.0)
                .fromCurrencyId("USD")
                .toCurrencyId("EUR")
                .transactionDate(LocalDate.now())
                .build();
    }
}