package eye.on.the.money.service.security;

import eye.on.the.money.dto.out.SecurityTransactionDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.security.Security;
import eye.on.the.money.model.security.SecurityTransaction;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.security.SecurityTransactionRepository;
import eye.on.the.money.service.user.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class SecurityTransactionServiceTest {

    @Mock
    private SecurityTransactionRepository securityTransactionRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private UserServiceImpl userService;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private SecurityService securityService;

    @InjectMocks
    private SecurityTransactionService securityTransactionService;

    private final User user = User.builder().id(1L).email("test@email.com").build();
    private final Currency currency = new Currency("EUR", "Euro");
    private final Security security = Security.builder().id("SEC1").name("Security One").build();

    private SecurityTransaction buildTransaction(Long id, String buySell, int quantity, double amount) {
        return SecurityTransaction.builder().id(id).buySell(buySell).quantity(quantity).amount(amount)
                .transactionDate(LocalDate.of(2025, 6, 1)).creationDate(LocalDate.of(2025, 6, 1))
                .currency(this.currency).security(this.security).user(this.user).build();
    }

    private SecurityTransactionDTO buildDTO(Long id, String buySell, int quantity, double amount) {
        return SecurityTransactionDTO.builder().transactionId(id).buySell(buySell).quantity(quantity).amount(amount)
                .transactionDate(LocalDate.of(2025, 6, 1)).currencyId("EUR").securityId("SEC1")
                .securityName("Security One").build();
    }

    @Test
    void getTransactions_returnsMappedDTOs() {
        SecurityTransaction tx = this.buildTransaction(1L, "B", 10, 500.0);
        SecurityTransactionDTO dto = this.buildDTO(1L, "B", 10, 500.0);
        when(this.securityTransactionRepository.findByUserEmailOrderByTransactionDateDesc("test@email.com")).thenReturn(List.of(tx));
        when(this.modelMapper.map(tx, SecurityTransactionDTO.class)).thenReturn(dto);

        List<SecurityTransactionDTO> result = this.securityTransactionService.getTransactions("test@email.com");

        assertEquals(1, result.size());
        assertEquals(500.0, result.get(0).getAmount());
    }

    @Test
    void getTransactions_returnsEmptyList() {
        when(this.securityTransactionRepository.findByUserEmailOrderByTransactionDateDesc("test@email.com")).thenReturn(List.of());

        List<SecurityTransactionDTO> result = this.securityTransactionService.getTransactions("test@email.com");

        assertTrue(result.isEmpty());
    }

    @Test
    void getCurrentHoldings_aggregatesBuysAndSells() {
        SecurityTransaction buy1 = this.buildTransaction(1L, "B", 10, 500.0);
        SecurityTransaction buy2 = this.buildTransaction(2L, "B", 5, 300.0);
        SecurityTransaction sell = this.buildTransaction(3L, "S", 3, 180.0);

        SecurityTransactionDTO buyDTO1 = this.buildDTO(1L, "B", 10, 500.0);
        SecurityTransactionDTO buyDTO2 = this.buildDTO(2L, "B", 5, 300.0);
        SecurityTransactionDTO sellDTO = this.buildDTO(3L, "S", 3, 180.0);

        when(this.securityTransactionRepository.findByUserEmailOrderByTransactionDate("test@email.com"))
                .thenReturn(List.of(buy1, buy2, sell));
        when(this.modelMapper.map(buy1, SecurityTransactionDTO.class)).thenReturn(buyDTO1);
        when(this.modelMapper.map(buy2, SecurityTransactionDTO.class)).thenReturn(buyDTO2);
        when(this.modelMapper.map(sell, SecurityTransactionDTO.class)).thenReturn(sellDTO);

        List<SecurityTransactionDTO> result = this.securityTransactionService.getCurrentHoldings("test@email.com");

        assertEquals(1, result.size());
        assertEquals(12, result.get(0).getQuantity());
        assertEquals(620.0, result.get(0).getAmount());
    }

    @Test
    void getCurrentHoldings_excludesFullySoldPositions() {
        SecurityTransaction buy = this.buildTransaction(1L, "B", 10, 500.0);
        SecurityTransaction sell = this.buildTransaction(2L, "S", 10, 500.0);

        SecurityTransactionDTO buyDTO = this.buildDTO(1L, "B", 10, 500.0);
        SecurityTransactionDTO sellDTO = this.buildDTO(2L, "S", 10, 500.0);

        when(this.securityTransactionRepository.findByUserEmailOrderByTransactionDate("test@email.com"))
                .thenReturn(List.of(buy, sell));
        when(this.modelMapper.map(buy, SecurityTransactionDTO.class)).thenReturn(buyDTO);
        when(this.modelMapper.map(sell, SecurityTransactionDTO.class)).thenReturn(sellDTO);

        List<SecurityTransactionDTO> result = this.securityTransactionService.getCurrentHoldings("test@email.com");

        assertTrue(result.isEmpty());
    }

    @Test
    void getCurrentHoldings_sortsByAmountDescending() {
        Security sec2 = Security.builder().id("SEC2").name("Security Two").build();
        SecurityTransaction tx1 = this.buildTransaction(1L, "B", 10, 200.0);
        SecurityTransaction tx2 = SecurityTransaction.builder().id(2L).buySell("B").quantity(5).amount(1000.0)
                .transactionDate(LocalDate.of(2025, 6, 1)).creationDate(LocalDate.of(2025, 6, 1))
                .currency(this.currency).security(sec2).user(this.user).build();

        SecurityTransactionDTO dto1 = this.buildDTO(1L, "B", 10, 200.0);
        SecurityTransactionDTO dto2 = SecurityTransactionDTO.builder().transactionId(2L).buySell("B").quantity(5).amount(1000.0)
                .transactionDate(LocalDate.of(2025, 6, 1)).currencyId("EUR").securityId("SEC2").securityName("Security Two").build();

        when(this.securityTransactionRepository.findByUserEmailOrderByTransactionDate("test@email.com"))
                .thenReturn(List.of(tx1, tx2));
        when(this.modelMapper.map(tx1, SecurityTransactionDTO.class)).thenReturn(dto1);
        when(this.modelMapper.map(tx2, SecurityTransactionDTO.class)).thenReturn(dto2);

        List<SecurityTransactionDTO> result = this.securityTransactionService.getCurrentHoldings("test@email.com");

        assertEquals(2, result.size());
        assertEquals(1000.0, result.get(0).getAmount());
        assertEquals(200.0, result.get(1).getAmount());
    }

    @Test
    void createTransaction_savesAndReturnsDTO() {
        SecurityTransactionDTO inputDTO = this.buildDTO(null, "B", 10, 500.0);
        SecurityTransaction savedTx = this.buildTransaction(1L, "B", 10, 500.0);
        SecurityTransactionDTO outputDTO = this.buildDTO(1L, "B", 10, 500.0);

        when(this.currencyRepository.findById("EUR")).thenReturn(Optional.of(this.currency));
        when(this.securityService.getOrCreateSecurity("SEC1", "Security One")).thenReturn(this.security);
        when(this.userService.loadUserByEmail("test@email.com")).thenReturn(this.user);
        when(this.securityTransactionRepository.save(any(SecurityTransaction.class))).thenReturn(savedTx);
        when(this.modelMapper.map(savedTx, SecurityTransactionDTO.class)).thenReturn(outputDTO);

        SecurityTransactionDTO result = this.securityTransactionService.createTransaction(inputDTO, "test@email.com");

        assertEquals(1L, result.getTransactionId());
        verify(this.securityTransactionRepository, times(1)).save(any(SecurityTransaction.class));
    }

    @Test
    void createTransaction_throwsWhenCurrencyNotFound() {
        SecurityTransactionDTO inputDTO = this.buildDTO(null, "B", 10, 500.0);
        when(this.currencyRepository.findById("EUR")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> this.securityTransactionService.createTransaction(inputDTO, "test@email.com"));
    }

    @Test
    void updateTransaction_updatesAndReturnsDTO() {
        SecurityTransactionDTO inputDTO = this.buildDTO(1L, "B", 15, 750.0);
        SecurityTransaction existingTx = this.buildTransaction(1L, "B", 10, 500.0);
        SecurityTransactionDTO outputDTO = this.buildDTO(1L, "B", 15, 750.0);

        when(this.currencyRepository.findById("EUR")).thenReturn(Optional.of(this.currency));
        when(this.securityService.getOrCreateSecurity("SEC1", "Security One")).thenReturn(this.security);
        when(this.securityTransactionRepository.findByIdAndUserEmail(1L, "test@email.com")).thenReturn(Optional.of(existingTx));
        when(this.modelMapper.map(existingTx, SecurityTransactionDTO.class)).thenReturn(outputDTO);

        SecurityTransactionDTO result = this.securityTransactionService.updateTransaction(inputDTO, "test@email.com");

        assertEquals(1L, result.getTransactionId());
        assertEquals(15, result.getQuantity());
    }

    @Test
    void updateTransaction_throwsWhenTransactionNotFound() {
        SecurityTransactionDTO inputDTO = this.buildDTO(1L, "B", 10, 500.0);
        when(this.currencyRepository.findById("EUR")).thenReturn(Optional.of(this.currency));
        when(this.securityService.getOrCreateSecurity("SEC1", "Security One")).thenReturn(this.security);
        when(this.securityTransactionRepository.findByIdAndUserEmail(1L, "test@email.com")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> this.securityTransactionService.updateTransaction(inputDTO, "test@email.com"));
    }

    @Test
    void deleteTransactionById_delegatesToRepository() {
        List<Long> ids = List.of(1L, 2L);
        doNothing().when(this.securityTransactionRepository).deleteByUserEmailAndIdIn("test@email.com", ids);

        this.securityTransactionService.deleteTransactionById("test@email.com", ids);

        verify(this.securityTransactionRepository, times(1)).deleteByUserEmailAndIdIn("test@email.com", ids);
    }
}
