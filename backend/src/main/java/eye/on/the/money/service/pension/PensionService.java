package eye.on.the.money.service.pension;

import eye.on.the.money.dto.out.PensionDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.pension.Pension;
import eye.on.the.money.repository.pension.PensionRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class PensionService {

    public static final String DEFAULT_CURRENCY = "HUF";

    private final PensionRepository pensionRepository;
    private final CurrencyRepository currencyRepository;
    private final UserService userService;

    public PensionDTO getPension(Long userId) {
        return this.pensionRepository.findByUserId(userId)
                .map(pension -> this.toDTO(pension.getTotalContribution(), pension.getCurrentValue(),
                        this.currencyIdOf(pension)))
                .orElseGet(() -> this.toDTO(0.0, 0.0, DEFAULT_CURRENCY));
    }

    @Transactional
    public PensionDTO updatePension(Long userId, PensionDTO pensionDTO) {
        Currency currency = this.resolveCurrency(pensionDTO.getCurrency());
        Pension pension = this.pensionRepository.findByUserId(userId).orElseGet(() -> {
            User user = this.userService.getReference(userId);
            return Pension.builder().user(user).build();
        });
        pension.setTotalContribution(pensionDTO.getTotalContribution());
        pension.setCurrentValue(pensionDTO.getCurrentValue());
        pension.setCurrency(currency);
        pension = this.pensionRepository.save(pension);
        return this.toDTO(pension.getTotalContribution(), pension.getCurrentValue(), this.currencyIdOf(pension));
    }

    private Currency resolveCurrency(String currencyId) {
        String id = (currencyId == null || currencyId.isBlank()) ? DEFAULT_CURRENCY : currencyId;
        return this.currencyRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Currency not found: " + id));
    }

    private String currencyIdOf(Pension pension) {
        return (pension.getCurrency() == null) ? DEFAULT_CURRENCY : pension.getCurrency().getId();
    }

    private PensionDTO toDTO(Double totalContribution, Double currentValue, String currency) {
        return PensionDTO.builder()
                .totalContribution(totalContribution)
                .currentValue(currentValue)
                .currency(currency)
                .build();
    }
}
