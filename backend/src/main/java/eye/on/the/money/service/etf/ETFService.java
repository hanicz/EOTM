package eye.on.the.money.service.etf;

import eye.on.the.money.model.etf.ETF;
import eye.on.the.money.repository.etf.ETFRepository;
import eye.on.the.money.util.Ticker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ETFService {

    private final ETFRepository etfRepository;

    public ETF getOrCreateETF(String shortName, String exchange, String name) {
        return this.etfRepository.findById(Ticker.id(shortName, exchange)).orElseGet(() -> {
                    ETF newETF = ETF.builder()
                            .id(Ticker.id(shortName, exchange))
                            .exchange(Ticker.normalizeExchange(exchange))
                            .shortName(Ticker.normalizeShortName(shortName))
                            .name(name)
                            .build();
                    return this.etfRepository.save(newETF);
                }
        );
    }
}
