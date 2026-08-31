package eye.on.the.money.controller;

import eye.on.the.money.dto.in.AccountEditDTO;
import eye.on.the.money.model.stock.Account;
import eye.on.the.money.service.stock.AccountService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    @Test
    public void getAccounts() {
        List<Account> accountList = new ArrayList<>();
        accountList.add(Account.builder().id(1L).build());
        accountList.add(Account.builder().id(2L).build());
        accountList.add(Account.builder().id(3L).build());

        when(this.accountService.getAccountsByUserId(anyLong())).thenReturn(accountList);

        ResponseEntity<List<Account>> result = this.accountController.getAccounts(1L);

        Assertions.assertIterableEquals(result.getBody(), accountList);
    }

    @Test
    public void deleteAccount() {
        when(this.accountService.deleteById(anyLong(), anyLong())).thenReturn(true);

        ResponseEntity<Void> result = this.accountController.deleteAccount(1L, 1L);

        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    public void deleteAccount404() {
        when(this.accountService.deleteById(anyLong(), anyLong())).thenReturn(false);

        ResponseEntity<Void> result = this.accountController.deleteAccount(1L, 1L);

        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    public void createAccount() {
        AccountEditDTO editDTO = new AccountEditDTO("Brokerage", LocalDate.of(2021, 3, 4));
        Account account = Account.builder().id(1L).accountName("Brokerage").build();
        when(this.accountService.createAccount(1L, editDTO)).thenReturn(account);

        ResponseEntity<Account> result = this.accountController.createAccount(1L, editDTO);

        Assertions.assertEquals(account, result.getBody());
    }

    @Test
    public void updateAccount() {
        AccountEditDTO editDTO = new AccountEditDTO("Retirement", LocalDate.of(2022, 5, 6));
        Account account = Account.builder().id(7L).accountName("Retirement").build();
        when(this.accountService.updateAccount(1L, 7L, editDTO)).thenReturn(account);

        ResponseEntity<Account> result = this.accountController.updateAccount(1L, 7L, editDTO);

        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertEquals(account, result.getBody());
    }
}
