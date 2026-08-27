package eye.on.the.money.service.stock;

import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Account;
import eye.on.the.money.repository.stock.AccountRepository;
import eye.on.the.money.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserService userService;

    public Account save(Account account) {
        return this.accountRepository.save(account);
    }

    public List<Account> getAccountsByUserEmail(String userEmail) {
        return this.accountRepository.findByUserEmailOrderByAccountName(userEmail);
    }

    public Account getAccount(String userEmail, Long accountId) {
        return this.accountRepository.findByUserEmailAndId(userEmail, accountId)
                .orElseThrow(() -> new NoSuchElementException("Account not found: " + accountId));
    }

    public Map<String, Long> getAccountIdsByName(String userEmail) {
        return this.accountRepository.findByUserEmailOrderByAccountName(userEmail).stream()
                .collect(Collectors.toMap(Account::getAccountName, Account::getId, (first, ignored) -> first));
    }

    @Transactional
    public boolean deleteById(String userEmail, Long id) {
        return this.accountRepository.deleteByUserEmailAndId(userEmail, id) > 0;
    }

    @Transactional
    public Account createAccount(Account account, String userEmail) {
        User user = this.userService.loadUserByEmail(userEmail);
        account.setId(null);
        account.setUser(user);
        return this.accountRepository.save(account);
    }
}
