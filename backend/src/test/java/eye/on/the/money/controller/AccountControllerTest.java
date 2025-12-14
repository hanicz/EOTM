package eye.on.the.money.controller;

import eye.on.the.money.model.User;
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

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

        when(this.accountService.getAccountsByUserEmail(anyString())).thenReturn(accountList);

        ResponseEntity<List<Account>> result = this.accountController.getAccounts(User.builder().id(1L).email("email").build());

        Assertions.assertIterableEquals(result.getBody(), accountList);
    }

    @Test
    public void deleteAccount() {
        when(this.accountService.deleteById(anyString(), anyLong())).thenReturn(true);

        ResponseEntity<Void> result = this.accountController.deleteAccount(User.builder().id(1L).email("email").build(), 1L);

        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    public void deleteAccount404() {
        when(this.accountService.deleteById(anyString(), anyLong())).thenReturn(false);

        ResponseEntity<Void> result = this.accountController.deleteAccount(User.builder().id(1L).email("email").build(), 1L);

        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    public void createAccount() {
        Account account = Account.builder().id(1L).build();
        User user = User.builder().id(1L).build();
        when(this.accountService.createAccount(account, user.getEmail())).thenReturn(account);

        ResponseEntity<Account> result = this.accountController.createAccount(account, user);

        Assertions.assertEquals(account, result.getBody());
    }
}