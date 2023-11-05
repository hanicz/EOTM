package eye.on.the.money.service.api.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.dto.out.*;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import eye.on.the.money.service.api.CurrencyConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class CurrencyConverterImpl implements CurrencyConverter {

    private static final Set<String> supportedCurrencies = Stream.of("EUR", "USD", "HUF").collect(Collectors.toCollection(HashSet::new));
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private ConfigRepository configRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @PostConstruct
    public void init() {
        CurrencyConverterImpl.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public void changeTransactionsCurrency(List<TransactionDTO> transactions, String toCurrency) {
        if (CurrencyConverterImpl.supportedCurrencies.contains(toCurrency)) {
            String currencyAPI = this.configRepository.findById("exchangerate").orElseThrow(NoSuchElementException::new).getConfigValue();
            Map<String, JsonNode> currencyMemory = new HashMap<>();
            transactions.stream().filter(t -> t.getAmount() != 0.0).forEach(transaction -> {
                if (!transaction.getCurrencyId().equals(toCurrency) && transaction.getLiveValue() == null) {
                    Double amount = transaction.getAmount();
                    String URL = this.createURL(currencyAPI, transaction.getCurrencyId(),
                            transaction.getTransactionDate(), transaction.getTransactionDate(), "timeseries");

                    JsonNode data = this.checkMemory(transaction.getCurrencyId() + CurrencyConverterImpl.dateFormat.format(transaction.getTransactionDate()),
                            currencyMemory, URL);
                    transaction.setAmount(amount *
                            data.get(CurrencyConverterImpl.dateFormat.format(transaction.getTransactionDate()))
                                    .get(toCurrency).doubleValue());
                }
                if (transaction.getLiveValue() != null) {
                    transaction.setValueDiff(transaction.getLiveValue() - transaction.getAmount());
                }
            });
        }
    }

    @Override
    public void forexWatchList(List<ForexWatchDTO> forexWatchList) {
        log.trace("Enter changeForexWatchList");
        String currencyAPI = this.configRepository.findById("eod").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("eod").orElseThrow(NoSuchElementException::new).getSecret();
        String joinedList = forexWatchList.stream().map(f -> (f.getFromCurrencyId() + f.getToCurrencyId() + ".FOREX")).collect(Collectors.joining(","));
        String URL = MessageFormat.format(currencyAPI + "/real-time/forex/?api_token={0}&fmt=json&s={1}", secret, joinedList);

        JsonNode responseBody = this.callCurrAPI(URL);
        for (JsonNode forex : responseBody) {
            Optional<ForexWatchDTO> forexWatchDTO = forexWatchList.stream().filter
                    (f -> (f.getFromCurrencyId() + f.getToCurrencyId() + ".FOREX").equals(forex.findValue("code").textValue())).findFirst();
            if (forexWatchDTO.isEmpty()) continue;
            forexWatchDTO.get().setLiveValue(forex.findValue("close").doubleValue());
            forexWatchDTO.get().setChange(forex.findValue("change").doubleValue() * -1);
            forexWatchDTO.get().setPChange(forex.findValue("change_p").doubleValue() * -1);
        }
    }

    @Override
    public void changeLiveValueCurrencyForForexTransactions(List<ForexTransactionDTO> forexTransactions) {
        log.trace("Enter changeForexWatchList");
        String currencyAPI = this.configRepository.findById("eod").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("eod").orElseThrow(NoSuchElementException::new).getSecret();
        String joinedList = forexTransactions.stream().map(f -> (f.getToCurrencyId() + f.getFromCurrencyId() + ".FOREX")).collect(Collectors.joining(","));
        String URL = MessageFormat.format(currencyAPI + "/real-time/forex/?api_token={0}&fmt=json&s={1}", secret, joinedList);

        JsonNode responseBody = this.callCurrAPI(URL);
        for (JsonNode forex : responseBody) {
            Optional<ForexTransactionDTO> forexTransactionDTO = forexTransactions.stream().filter
                    (f -> (f.getToCurrencyId() + f.getFromCurrencyId() + ".FOREX").equals(forex.findValue("code").textValue())).findFirst();
            if (forexTransactionDTO.isEmpty()) continue;
            forexTransactionDTO.get().setLiveValue(forex.findValue("close").doubleValue() * forexTransactionDTO.get().getToAmount());
            forexTransactionDTO.get().setLiveChangeRate(forex.findValue("close").doubleValue());
            forexTransactionDTO.get().setValueDiff(forexTransactionDTO.get().getLiveValue() - forexTransactionDTO.get().getFromAmount());
        }
    }

    private JsonNode checkMemory(String key, Map<String, JsonNode> memory, String URL) {
        JsonNode data;
        if (memory.containsKey(key)) {
            data = memory.get(key);
        } else {
            data = this.callCurrencyAPI(URL);
            memory.put(key, data);
        }
        return data;
    }

    @Retryable(value = APIException.class, maxAttempts = 3)
    private JsonNode callCurrencyAPI(String URL) {
        String apikey = this.credentialRepository.findById("exchange").orElseThrow(NoSuchElementException::new).getSecret();
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", apikey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(URL, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.getBody());
                return root.path("rates");
            } catch (JsonProcessingException | NullPointerException e) {
                throw new APIException("JSON process failed");
            }
        } else {
            throw new APIException("Unable to reach currency API");
        }
    }

    private JsonNode callCurrAPI(String URL) {
        log.trace("Enter callCurrAPI");
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(URL, String.class);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(response.getBody());
        } catch (JsonProcessingException | NullPointerException e) {
            log.error("JSON process failed");
            throw new APIException("JSON process failed");
        } catch (RestClientException e) {
            log.error("Unable to reach currency API: " + e.getMessage());
            throw new APIException("Unable to reach currency API. " + e.getMessage());
        }
    }

    private String createURL(String url, String fromCurrency, Date from, Date to, String type) {
        String dateFrom = CurrencyConverterImpl.dateFormat.format(from);
        String dateTo = CurrencyConverterImpl.dateFormat.format(to);
        return MessageFormat.format(
                url + "{0}?base={1}&start_date={2}&end_date={3}&symbols={4}",
                type, fromCurrency, dateFrom, dateTo, String.join(",", CurrencyConverterImpl.supportedCurrencies));
    }
}
