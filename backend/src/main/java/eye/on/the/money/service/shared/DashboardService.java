package eye.on.the.money.service.shared;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.out.DashboardRatesDTO;
import eye.on.the.money.service.api.EODAPIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardService {

    private static final String BASE_CURRENCY = "EUR";

    private final EODAPIService eodAPIService;

    public DashboardRatesDTO getConversionRates(List<String> currencies) {
        log.trace("Enter");
        Set<String> targetCurrencies = currencies.stream()
                .map(String::toUpperCase)
                .filter(currency -> !BASE_CURRENCY.equals(currency))
                .collect(Collectors.toSet());

        Map<String, Double> rates = new HashMap<>();
        if (targetCurrencies.isEmpty()) {
            return DashboardRatesDTO.builder().rates(rates).build();
        }

        String tickers = targetCurrencies.stream()
                .map(currency -> BASE_CURRENCY + currency + ".FOREX")
                .collect(Collectors.joining(","));

        JsonNode responseBody = this.eodAPIService.getLiveForexValue(tickers);
        for (JsonNode forex : responseBody) {
            String code = forex.findValue("code").textValue();
            double close = forex.findValue("close").doubleValue();
            for (String currency : targetCurrencies) {
                if (code.startsWith(BASE_CURRENCY + currency)) {
                    rates.put(currency, close);
                    break;
                }
            }
        }

        return DashboardRatesDTO.builder().rates(rates).build();
    }
}
