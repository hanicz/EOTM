package eye.on.the.money.service.forex;

import eye.on.the.money.dto.out.CurrencyDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.repository.forex.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyRepository currencyRepository;

    public List<CurrencyDTO> getCurrencies() {
        return this.currencyRepository.findAll().stream()
                .sorted(Comparator.comparing(Currency::getId))
                .map(this::toDTO)
                .toList();
    }

    private CurrencyDTO toDTO(Currency currency) {
        return CurrencyDTO.builder()
                .id(currency.getId())
                .name(currency.getName())
                .build();
    }
}
