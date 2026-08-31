package eye.on.the.money.service.stock;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.in.AccountEditDTO;
import eye.on.the.money.exception.ValidationException;
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
import java.util.NoSuchElementException;

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
    public void getAccountsByUserId() {
        List<Account> result = this.accountService.getAccountsByUserId(this.user.getId());
        int actualSize = this.accountRepository.findByUserIdOrderByAccountName(this.user.getId()).size();
        Assertions.assertEquals(actualSize, result.size());
    }

    @Test
    public void deleteById() {
        boolean result = this.accountService.deleteById(this.user.getId(), 1L);
        Assertions.assertTrue(result);
    }

    @Test
    public void createAccount() {
        AccountEditDTO editDTO = new AccountEditDTO("Create Fixture", LocalDate.of(2021, 4, 5));

        Account result = this.accountService.createAccount(this.user.getId(), editDTO);

        Account dbResult = this.accountRepository.findByUserIdAndId(this.user.getId(), result.getId()).get();
        Assertions.assertEquals("Create Fixture", dbResult.getAccountName());
        Assertions.assertEquals(LocalDate.of(2021, 4, 5), dbResult.getCreationDate());
        Assertions.assertEquals(this.user.getId(), dbResult.getUser().getId());
    }

    @Test
    public void createAccountRejectsDuplicateName() {
        AccountEditDTO editDTO = new AccountEditDTO("Duplicate Create Fixture", LocalDate.of(2021, 4, 5));
        this.accountService.createAccount(this.user.getId(), editDTO);

        Assertions.assertThrows(ValidationException.class,
                () -> this.accountService.createAccount(this.user.getId(), editDTO));
    }

    @Test
    public void updateAccount() {
        Account account = this.accountService.createAccount(this.user.getId(),
                new AccountEditDTO("Rename Source Fixture", LocalDate.of(2020, 1, 2)));

        Account result = this.accountService.updateAccount(this.user.getId(), account.getId(),
                new AccountEditDTO("  Rename Target Fixture  ", LocalDate.of(2023, 7, 8)));

        Account dbResult = this.accountRepository.findByUserIdAndId(this.user.getId(), result.getId()).get();
        Assertions.assertEquals("Rename Target Fixture", dbResult.getAccountName());
        Assertions.assertEquals(LocalDate.of(2023, 7, 8), dbResult.getCreationDate());
    }

    @Test
    public void updateAccountRejectsDuplicateName() {
        this.accountService.createAccount(this.user.getId(),
                new AccountEditDTO("Taken Name Fixture", LocalDate.of(2020, 1, 2)));
        Account other = this.accountService.createAccount(this.user.getId(),
                new AccountEditDTO("Other Name Fixture", LocalDate.of(2020, 1, 2)));

        AccountEditDTO editDTO = new AccountEditDTO("Taken Name Fixture", LocalDate.of(2020, 1, 2));

        Assertions.assertThrows(ValidationException.class,
                () -> this.accountService.updateAccount(this.user.getId(), other.getId(), editDTO));
    }

    @Test
    public void updateAccountToItsOwnNameIsAllowed() {
        Account account = this.accountService.createAccount(this.user.getId(),
                new AccountEditDTO("Own Name Fixture", LocalDate.of(2020, 1, 2)));

        Account result = this.accountService.updateAccount(this.user.getId(), account.getId(),
                new AccountEditDTO("Own Name Fixture", LocalDate.of(2024, 9, 10)));

        Assertions.assertEquals("Own Name Fixture", result.getAccountName());
        Assertions.assertEquals(LocalDate.of(2024, 9, 10), result.getCreationDate());
    }

    @Test
    public void updateAccountNotFound() {
        AccountEditDTO editDTO = new AccountEditDTO("Missing Fixture", LocalDate.of(2020, 1, 2));

        Assertions.assertThrows(NoSuchElementException.class,
                () -> this.accountService.updateAccount(this.user.getId(), 9999L, editDTO));
    }
}
