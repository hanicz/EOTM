package eye.on.the.money.service.currency.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.service.currency.CurrencyConverter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CurrencyConverterImpl implements CurrencyConverter {

    private static final Set<String> supportedCurrencies = Stream.of("EUR", "USD", "HUF").collect(Collectors.toCollection(HashSet::new));
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public void changeInvestmentsToCurrency(List<InvestmentDTO> investments, String toCurrency) {
        if (CurrencyConverterImpl.supportedCurrencies.contains(toCurrency)) {
            for (InvestmentDTO investment : investments) {
                if (investment.getCurrencyId().equals(toCurrency)) continue;
                Double amount = investment.getAmount();
                investment.setAmount(amount * this.callCurrencyAPI(investment.getCurrencyId(), toCurrency, investment.getTransactionDate()));
                investment.setCurrencyId(toCurrency);
            }
        }

    }

    @Retryable(value = APIException.class, maxAttempts = 3)
    private Double callCurrencyAPI(String fromCurrency, String toCurrency, Date investmentDate) {
        RestTemplate restTemplate = new RestTemplate();
        String apiKey = "";
        String date = CurrencyConverterImpl.dateFormat.format(investmentDate);
        String URL = MessageFormat.format(
                "https://freecurrencyapi.net/api/v2/historical?apikey={0}&base_currency={1}&date_from={2}&date_to={3}",
                apiKey, fromCurrency, date, date);

        ResponseEntity<String> response = restTemplate.getForEntity(URL, String.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.getBody());
                JsonNode name = root.path("data").get(date).get(toCurrency);
                return name.doubleValue();
            } catch (JsonProcessingException e) {
                throw new APIException("JSON process exception");
            }
        } else {
            throw new APIException("Unable to reach API");
        }
    }
}
