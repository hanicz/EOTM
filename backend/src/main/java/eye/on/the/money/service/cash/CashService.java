package eye.on.the.money.service.cash;

import eye.on.the.money.dto.out.CashDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.cash.Cash;
import eye.on.the.money.repository.cash.CashRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@Slf4j
@RequiredArgsConstructor
public class CashService {

    public static final String DEFAULT_CURRENCY = "HUF";

    private final CashRepository cashRepository;
    private final CurrencyRepository currencyRepository;
    private final UserService userService;

    public CashDTO getCash(Long userId) {
        log.trace("Enter");
        return this.cashRepository.findByUserId(userId)
                .map(cash -> this.toDTO(cash.getAmount(), this.currencyIdOf(cash)))
                .orElseGet(() -> this.toDTO(0.0, DEFAULT_CURRENCY));
    }

    @Transactional
    public CashDTO updateCash(Long userId, CashDTO cashDTO) {
        log.trace("Enter");
        Currency currency = this.resolveCurrency(cashDTO.getCurrency());
        Cash cash = this.cashRepository.findByUserId(userId).orElseGet(() -> {
            User user = this.userService.getReference(userId);
            return Cash.builder().user(user).build();
        });
        cash.setAmount(cashDTO.getAmount());
        cash.setCurrency(currency);
        cash = this.cashRepository.save(cash);
        return this.toDTO(cash.getAmount(), this.currencyIdOf(cash));
    }

    private Currency resolveCurrency(String currencyId) {
        String id = (currencyId == null || currencyId.isBlank()) ? DEFAULT_CURRENCY : currencyId;
        return this.currencyRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Currency not found: " + id));
    }

    private String currencyIdOf(Cash cash) {
        return (cash.getCurrency() == null) ? DEFAULT_CURRENCY : cash.getCurrency().getId();
    }

    private CashDTO toDTO(Double amount, String currency) {
        return CashDTO.builder().amount(amount).currency(currency).build();
    }
}
