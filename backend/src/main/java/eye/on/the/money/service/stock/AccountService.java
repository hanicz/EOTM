package eye.on.the.money.service.stock;

import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Account;
import eye.on.the.money.repository.stock.AccountRepository;
import eye.on.the.money.service.user.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserServiceImpl userService;

    public Account save(Account account) {
        return this.accountRepository.save(account);
    }

    public List<Account> getAccountsByUserEmail(String userEmail) {
        return this.accountRepository.findByUserEmailOrderByAccountName(userEmail);
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
