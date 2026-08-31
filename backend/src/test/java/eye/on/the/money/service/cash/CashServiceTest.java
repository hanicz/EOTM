package eye.on.the.money.service.cash;

import eye.on.the.money.dto.out.CashDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.cash.Cash;
import eye.on.the.money.repository.cash.CashRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.service.user.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CashServiceTest {

    private static final Long USER_ID = 42L;

    @Mock
    private CashRepository cashRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private UserService userService;
    @InjectMocks
    private CashService cashService;

    private final User user = User.builder().id(USER_ID).email("cash@example.com").build();
    private final Currency huf = new Currency("HUF", "forint");
    private final Currency eur = new Currency("EUR", "euro");

    @Test
    void getCash_returnsZeroInTheDefaultCurrencyWhenNoRowExists() {
        when(this.cashRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        CashDTO result = this.cashService.getCash(USER_ID);

        Assertions.assertEquals(0.0, result.getAmount());
        Assertions.assertEquals("HUF", result.getCurrency());
    }

    @Test
    void getCash_returnsStoredAmountAndCurrency() {
        when(this.cashRepository.findByUserId(USER_ID)).thenReturn(Optional.of(
                Cash.builder().id(1L).amount(750000.0).currency(this.eur).user(this.user).build()));

        CashDTO result = this.cashService.getCash(USER_ID);

        Assertions.assertEquals(750000.0, result.getAmount());
        Assertions.assertEquals("EUR", result.getCurrency());
    }

    @Test
    void getCash_fallsBackToTheDefaultCurrencyForRowsWithoutOne() {
        when(this.cashRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(Cash.builder().id(1L).amount(750000.0).user(this.user).build()));

        CashDTO result = this.cashService.getCash(USER_ID);

        Assertions.assertEquals(750000.0, result.getAmount());
        Assertions.assertEquals("HUF", result.getCurrency());
    }

    @Test
    void updateCash_createsRowWhenAbsent() {
        when(this.currencyRepository.findById("EUR")).thenReturn(Optional.of(this.eur));
        when(this.cashRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(this.userService.getReference(USER_ID)).thenReturn(this.user);
        when(this.cashRepository.save(any(Cash.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashDTO result = this.cashService.updateCash(USER_ID,
                CashDTO.builder().amount(320000.0).currency("EUR").build());

        ArgumentCaptor<Cash> captor = ArgumentCaptor.forClass(Cash.class);
        verify(this.cashRepository).save(captor.capture());
        Assertions.assertEquals(320000.0, captor.getValue().getAmount());
        Assertions.assertEquals(this.eur, captor.getValue().getCurrency());
        Assertions.assertEquals(this.user, captor.getValue().getUser());
        Assertions.assertEquals(320000.0, result.getAmount());
        Assertions.assertEquals("EUR", result.getCurrency());
    }

    @Test
    void updateCash_updatesExistingRow() {
        Cash existing = Cash.builder().id(7L).amount(100000.0).currency(this.huf).user(this.user).build();
        when(this.currencyRepository.findById("EUR")).thenReturn(Optional.of(this.eur));
        when(this.cashRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(this.cashRepository.save(existing)).thenReturn(existing);

        CashDTO result = this.cashService.updateCash(USER_ID,
                CashDTO.builder().amount(900000.0).currency("EUR").build());

        Assertions.assertEquals(900000.0, existing.getAmount());
        Assertions.assertEquals(this.eur, existing.getCurrency());
        Assertions.assertEquals(7L, existing.getId());
        Assertions.assertEquals(900000.0, result.getAmount());
        Assertions.assertEquals("EUR", result.getCurrency());
        verify(this.userService, never()).getReference(USER_ID);
    }

    @Test
    void updateCash_defaultsToTheDefaultCurrencyWhenNoneIsGiven() {
        Cash existing = Cash.builder().id(7L).amount(100000.0).user(this.user).build();
        when(this.currencyRepository.findById("HUF")).thenReturn(Optional.of(this.huf));
        when(this.cashRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(this.cashRepository.save(existing)).thenReturn(existing);

        CashDTO result = this.cashService.updateCash(USER_ID, CashDTO.builder().amount(900000.0).build());

        Assertions.assertEquals(this.huf, existing.getCurrency());
        Assertions.assertEquals("HUF", result.getCurrency());
    }

    @Test
    void updateCash_rejectsAnUnknownCurrency() {
        when(this.currencyRepository.findById("XYZ")).thenReturn(Optional.empty());

        CashDTO request = CashDTO.builder().amount(900000.0).currency("XYZ").build();

        NoSuchElementException exception = Assertions.assertThrows(NoSuchElementException.class,
                () -> this.cashService.updateCash(USER_ID, request));

        Assertions.assertEquals("Currency not found: XYZ", exception.getMessage());
        verify(this.cashRepository, never()).save(any(Cash.class));
    }
}
