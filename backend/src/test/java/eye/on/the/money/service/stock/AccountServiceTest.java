package eye.on.the.money.service.stock;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Account;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.repository.stock.AccountRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.List;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class AccountServiceTest {
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    public void init() {
        this.user = this.userRepository.findByEmail("test@test.test");
    }

    @Test
    public void getAccountsByUserEmail() {
        List<Account> result = this.accountService.getAccountsByUserEmail(this.user.getEmail());
        int actualSize = this.accountRepository.findByUserEmailOrderByAccountName(this.user.getEmail()).size();
        Assertions.assertEquals(actualSize, result.size());
    }

    @Test
    public void deleteById() {
        boolean result = this.accountService.deleteById(this.user.getEmail(), 1L);
        Assertions.assertTrue(result);
    }

    @Test
    public void createAccount() {
        Account account = Account.builder().accountName("Test Account").creationDate(LocalDate.now()).user(this.user).build();

        Account result = this.accountService.createAccount(account, this.user.getEmail());
        Account dbResult = this.accountRepository.findByUserEmailAndId(this.user.getEmail(), result.getId()).get();
        Assertions.assertEquals(dbResult.getAccountName(), account.getAccountName());
        Assertions.assertEquals(dbResult.getCreationDate(), account.getCreationDate());
        Assertions.assertEquals(dbResult.getUser().getId(), account.getUser().getId());
    }
}