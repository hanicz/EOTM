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

    public List<Account> getAccountsByUserId(Long userId) {
        return this.accountRepository.findByUserIdOrderByAccountName(userId);
    }

    public Account getAccount(Long userId, Long accountId) {
        return this.accountRepository.findByUserIdAndId(userId, accountId)
                .orElseThrow(() -> new NoSuchElementException("Account not found: " + accountId));
    }

    public Map<String, Long> getAccountIdsByName(Long userId) {
        return this.accountRepository.findByUserIdOrderByAccountName(userId).stream()
                .collect(Collectors.toMap(Account::getAccountName, Account::getId, (first, ignored) -> first));
    }

    @Transactional
    public boolean deleteById(Long userId, Long id) {
        return this.accountRepository.deleteByUserIdAndId(userId, id) > 0;
    }

    @Transactional
    public Account createAccount(Account account, Long userId) {
        User user = this.userService.getReference(userId);
        account.setId(null);
        account.setUser(user);
        return this.accountRepository.save(account);
    }
}
