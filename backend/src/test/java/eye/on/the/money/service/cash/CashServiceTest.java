package eye.on.the.money.service.cash;

import eye.on.the.money.dto.out.CashDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.cash.Cash;
import eye.on.the.money.repository.cash.CashRepository;
import eye.on.the.money.service.user.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

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
    private UserService userService;
    @InjectMocks
    private CashService cashService;

    private final User user = User.builder().id(USER_ID).email("cash@example.com").build();

    @Test
    void getCash_returnsZeroWhenNoRowExists() {
        when(this.cashRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        CashDTO result = this.cashService.getCash(USER_ID);

        Assertions.assertEquals(0.0, result.getAmount());
        Assertions.assertEquals("HUF", result.getCurrency());
    }

    @Test
    void getCash_returnsStoredAmount() {
        when(this.cashRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(Cash.builder().id(1L).amount(750000.0).user(this.user).build()));

        CashDTO result = this.cashService.getCash(USER_ID);

        Assertions.assertEquals(750000.0, result.getAmount());
        Assertions.assertEquals("HUF", result.getCurrency());
    }

    @Test
    void updateCash_createsRowWhenAbsent() {
        when(this.cashRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(this.userService.getReference(USER_ID)).thenReturn(this.user);
        when(this.cashRepository.save(any(Cash.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashDTO result = this.cashService.updateCash(USER_ID, CashDTO.builder().amount(320000.0).build());

        ArgumentCaptor<Cash> captor = ArgumentCaptor.forClass(Cash.class);
        verify(this.cashRepository).save(captor.capture());
        Assertions.assertEquals(320000.0, captor.getValue().getAmount());
        Assertions.assertEquals(this.user, captor.getValue().getUser());
        Assertions.assertEquals(320000.0, result.getAmount());
    }

    @Test
    void updateCash_updatesExistingRow() {
        Cash existing = Cash.builder().id(7L).amount(100000.0).user(this.user).build();
        when(this.cashRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(this.cashRepository.save(existing)).thenReturn(existing);

        CashDTO result = this.cashService.updateCash(USER_ID, CashDTO.builder().amount(900000.0).build());

        Assertions.assertEquals(900000.0, existing.getAmount());
        Assertions.assertEquals(7L, existing.getId());
        Assertions.assertEquals(900000.0, result.getAmount());
        verify(this.userService, never()).getReference(USER_ID);
    }
}
