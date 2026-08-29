package eye.on.the.money.service.cash;

import eye.on.the.money.dto.out.CashDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.cash.Cash;
import eye.on.the.money.repository.cash.CashRepository;
import eye.on.the.money.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CashService {

    public static final String CURRENCY = "HUF";

    private final CashRepository cashRepository;
    private final UserService userService;

    public CashDTO getCash(Long userId) {
        log.trace("Enter");
        double amount = this.cashRepository.findByUserId(userId).map(Cash::getAmount).orElse(0.0);
        return CashDTO.builder().amount(amount).currency(CURRENCY).build();
    }

    @Transactional
    public CashDTO updateCash(Long userId, CashDTO cashDTO) {
        log.trace("Enter");
        Cash cash = this.cashRepository.findByUserId(userId).orElseGet(() -> {
            User user = this.userService.getReference(userId);
            return Cash.builder().user(user).build();
        });
        cash.setAmount(cashDTO.getAmount());
        cash = this.cashRepository.save(cash);
        return CashDTO.builder().amount(cash.getAmount()).currency(CURRENCY).build();
    }
}
