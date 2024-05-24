package eye.on.the.money.service.stock;

import eye.on.the.money.model.Account;
import eye.on.the.money.repository.stock.AccountRepository;
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

    public Account save(Account account) {
        return this.accountRepository.save(account);
    }

    public List<Account> getAccountsByUserEmail(String userEmail) {
        return this.accountRepository.findByUserEmailOrderByAccountName(userEmail);
    }

    @Transactional
    public void deleteById(String userEmail, Long id) {
        this.accountRepository.deleteByUserEmailAndId(userEmail, id);
    }

    @Transactional
    public Account createAccount(Account account) {
        account.setId(null);
        return this.accountRepository.save(account);
    }

    @Transactional
    private Account updateAccount(Account account) {
        return this.accountRepository.save(account);
    }
}
