package eye.on.the.money.service.currency.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import eye.on.the.money.service.currency.CurrencyConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CurrencyConverterImpl implements CurrencyConverter {

    private static final Set<String> supportedCurrencies = Stream.of("EUR", "USD", "HUF").collect(Collectors.toCollection(HashSet::new));
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private ConfigRepository configRepository;


    @Autowired
    private CredentialRepository credentialRepository;

    @Override
    public void changeInvestmentsCurrency(List<InvestmentDTO> investments, String toCurrency) {
        if (CurrencyConverterImpl.supportedCurrencies.contains(toCurrency)) {
            String currencyAPI = this.configRepository.findById("freecurrencyapi").orElseThrow(NoSuchElementException::new).getConfigValue();
            String secret = this.credentialRepository.findById("freecurrencyapi").orElseThrow(NoSuchElementException::new).getSecret();
            investments.stream().filter(i -> !i.getCurrencyId().equals(toCurrency)).forEach(investment -> {
                Double amount = investment.getAmount();
                String URL = this.createURL(currencyAPI, secret, investment.getCurrencyId(), investment.getTransactionDate());
                investment.setAmount(amount * this.callCurrencyAPI(URL, toCurrency, investment.getTransactionDate()));
            });
        }

    }

    @Override
    public void changeLiveValueCurrency(List<InvestmentDTO> investments, String toCurrency) {
        if (CurrencyConverterImpl.supportedCurrencies.contains(toCurrency)) {
            String currencyAPI = this.configRepository.findById("freecurrencyapi").orElseThrow(NoSuchElementException::new).getConfigValue();
            String secret = this.credentialRepository.findById("freecurrencyapi").orElseThrow(NoSuchElementException::new).getSecret();
            investments.forEach(investment -> {
                if (!investment.getCurrencyId().equals(toCurrency) && investment.getLiveValue() != null) {
                    String URL = this.createURL(currencyAPI, secret, investment.getCurrencyId(), new Date());
                    investment.setLiveValue(investment.getLiveValue() * this.callCurrencyAPI(URL, toCurrency, new Date()));
                }
                if (investment.getLiveValue() != null) {
                    investment.setValueDiff(investment.getLiveValue() - investment.getAmount());
                }
                investment.setCurrencyId(toCurrency);
            });
        }
    }

    @Retryable(value = APIException.class, maxAttempts = 3)
    private Double callCurrencyAPI(String URL, String toCurrency, Date investmentDate) {
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(URL, String.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.getBody());
                JsonNode name = root.path("data").get(CurrencyConverterImpl.dateFormat.format(investmentDate)).get(toCurrency);
                return name.doubleValue();
            } catch (JsonProcessingException | NullPointerException e) {
                throw new APIException("JSON process failed");
            }
        } else {
            throw new APIException("Unable to reach currency API");
        }
    }

    private String createURL(String url, String secret, String fromCurrency, Date investmentDate) {
        String date = CurrencyConverterImpl.dateFormat.format(investmentDate);
        return MessageFormat.format(
                url + "historical?apikey={0}&base_currency={1}&date_from={2}&date_to={3}",
                secret, fromCurrency, date, date);
    }
}
