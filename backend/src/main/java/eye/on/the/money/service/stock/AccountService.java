package eye.on.the.money.service.stock;

import eye.on.the.money.dto.in.AccountEditDTO;
import eye.on.the.money.exception.ValidationException;
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

    private static final String DUPLICATE_MESSAGE = "An account with this name already exists";

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
    public Account createAccount(Long userId, AccountEditDTO editDTO) {
        String accountName = editDTO.accountName().trim();
        this.rejectDuplicate(userId, accountName, null);

        Account account = Account.builder()
                .accountName(accountName)
                .creationDate(editDTO.creationDate())
                .user(this.userService.getReference(userId))
                .build();

        return this.accountRepository.save(account);
    }

    @Transactional
    public Account updateAccount(Long userId, Long id, AccountEditDTO editDTO) {
        String accountName = editDTO.accountName().trim();
        Account account = this.getAccount(userId, id);
        this.rejectDuplicate(userId, accountName, id);

        account.setAccountName(accountName);
        account.setCreationDate(editDTO.creationDate());

        return this.accountRepository.save(account);
    }

    private void rejectDuplicate(Long userId, String accountName, Long selfId) {
        this.accountRepository.findByUserIdAndAccountName(userId, accountName)
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new ValidationException(DUPLICATE_MESSAGE);
                });
    }
}
