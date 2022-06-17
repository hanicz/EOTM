package eye.on.the.money.service.api.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.dto.out.ForexWatchDTO;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import eye.on.the.money.service.api.CurrencyConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
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
    public void changeInvestmentsCurrency(List<InvestmentDTO> investments, String toCurrency) {
        if (CurrencyConverterImpl.supportedCurrencies.contains(toCurrency)) {
            String currencyAPI = this.configRepository.findById("exchangerate").orElseThrow(NoSuchElementException::new).getConfigValue();
            Map<String, JsonNode> currencyMemory = new HashMap<>();
            investments.stream().filter(i -> !i.getCurrencyId().equals(toCurrency)).forEach(investment -> {
                Double amount = investment.getAmount();
                String URL = this.createURL(currencyAPI, investment.getCurrencyId(), investment.getTransactionDate(),
                        investment.getTransactionDate(), "timeseries");
                JsonNode data = this.checkMemory(investment.getCurrencyId() + CurrencyConverterImpl.dateFormat.format(investment.getTransactionDate()),
                        currencyMemory, URL);

                investment.setAmount(amount *
                        data.get(CurrencyConverterImpl.dateFormat.format(investment.getTransactionDate()))
                                .get(toCurrency).doubleValue());
            });
        }
    }

    @Override
    public void changeETFCurrency(List<ETFInvestmentDTO> investments, String toCurrency) {
        if (CurrencyConverterImpl.supportedCurrencies.contains(toCurrency)) {
            String currencyAPI = this.configRepository.findById("exchangerate").orElseThrow(NoSuchElementException::new).getConfigValue();
            Map<String, JsonNode> currencyMemory = new HashMap<>();
            investments.stream().filter(i -> !i.getCurrencyId().equals(toCurrency)).forEach(investment -> {
                Double amount = investment.getAmount();
                String URL = this.createURL(currencyAPI, investment.getCurrencyId(), investment.getTransactionDate(),
                        investment.getTransactionDate(), "timeseries");
                JsonNode data = this.checkMemory(investment.getCurrencyId() + CurrencyConverterImpl.dateFormat.format(investment.getTransactionDate()),
                        currencyMemory, URL);
                investment.setAmount(amount *
                        data.get(CurrencyConverterImpl.dateFormat.format(investment.getTransactionDate()))
                                .get(toCurrency).doubleValue());

                URL = this.createURL(currencyAPI, investment.getCurrencyId(), new Date(), new Date(), "timeseries");
                data = this.checkMemory(investment.getCurrencyId() + CurrencyConverterImpl.dateFormat.format(new Date()), currencyMemory, URL);
                investment.setLiveValue(investment.getLiveValue() * investment.getQuantity() * data.get(toCurrency).doubleValue());
                investment.setValueDiff(investment.getLiveValue() - investment.getAmount());
            });
        }
    }

    @Override
    public void changeLiveValueCurrency(List<InvestmentDTO> investments, String toCurrency) {
        if (CurrencyConverterImpl.supportedCurrencies.contains(toCurrency)) {
            String currencyAPI = this.configRepository.findById("exchangerate").orElseThrow(NoSuchElementException::new).getConfigValue();
            Map<String, JsonNode> currencyMemory = new HashMap<>();
            investments.forEach(investment -> {
                if (!investment.getCurrencyId().equals(toCurrency) && investment.getLiveValue() != null) {
                    String URL = this.createURL(currencyAPI, investment.getCurrencyId(), new Date(), new Date(), "timeseries");
                    JsonNode data = this.checkMemory(investment.getCurrencyId(), currencyMemory, URL);
                    investment.setLiveValue(investment.getLiveValue() * data.get(CurrencyConverterImpl.dateFormat.format(new Date())).get(toCurrency).doubleValue());
                }
                if (investment.getLiveValue() != null) {
                    investment.setValueDiff(investment.getLiveValue() - investment.getAmount());
                }
                investment.setCurrencyId(toCurrency);
            });
        }
    }

    @Override
    public void changeForexWatchList(List<ForexWatchDTO> forexWatchList) {
        String currencyAPI = this.configRepository.findById("exchangerate").orElseThrow(NoSuchElementException::new).getConfigValue();
        Date yesterday = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        Map<String, JsonNode> currencyMemory = new HashMap<>();
        forexWatchList.forEach(f -> {
            if (CurrencyConverterImpl.supportedCurrencies.containsAll(Arrays.asList(f.getToCurrencyId(), f.getFromCurrencyId()))) {
                String URL = this.createURL(currencyAPI, f.getFromCurrencyId(), yesterday, new Date(), "timeseries");
                JsonNode data = this.checkMemory(f.getFromCurrencyId(), currencyMemory, URL);

                Double liveValue = data.get(CurrencyConverterImpl.dateFormat.format(new Date())).get(f.getToCurrencyId()).doubleValue();
                Double yesterdayValue = data.get(CurrencyConverterImpl.dateFormat.format(yesterday)).get(f.getToCurrencyId()).doubleValue();
                f.setLiveValue(liveValue);
                f.setChange(yesterdayValue - liveValue);
                f.setPChange((yesterdayValue - liveValue) / yesterdayValue * 100);
            }
        });
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
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(URL, String.class);
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

    private String createURL(String url, String fromCurrency, Date from, Date to, String type) {
        String dateFrom = CurrencyConverterImpl.dateFormat.format(from);
        String dateTo = CurrencyConverterImpl.dateFormat.format(to);
        return MessageFormat.format(
                url + "{0}?base={1}&start_date={2}&end_date={3}&symbols={4}",
                type, fromCurrency, dateFrom, dateTo, String.join(",", CurrencyConverterImpl.supportedCurrencies));
    }
}
