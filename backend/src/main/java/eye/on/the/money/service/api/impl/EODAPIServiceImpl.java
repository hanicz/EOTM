package eye.on.the.money.service.api.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.dto.out.StockWatchDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.stock.EODCandleQuote;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.model.stock.Symbol;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import eye.on.the.money.service.api.EODAPIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EODAPIServiceImpl implements EODAPIService {

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private ConfigRepository configRepository;

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public void getLiveValue(List<InvestmentDTO> investmentDTOList) {
        log.trace("Enter getLiveValue");
        String stockAPI = this.configRepository.findById("eod").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("eod").orElseThrow(NoSuchElementException::new).getSecret();
        String joinedList = investmentDTOList.stream().map(i -> (i.getShortName() + "." + i.getExchange())).collect(Collectors.joining(","));
        String URL = MessageFormat.format(stockAPI + "/real-time/stock/?api_token={0}&fmt=json&s={1}", secret, joinedList);

        JsonNode responseBody = this.callStockAPI(URL);
        for (JsonNode stock : responseBody) {
            Optional<InvestmentDTO> investmentDTO = investmentDTOList.stream().filter
                    (i -> (i.getShortName() + "." + i.getExchange()).equals(stock.findValue("code").textValue())).findFirst();
            if (investmentDTO.isEmpty()) continue;
            investmentDTO.get().setLiveValue(stock.findValue("close").doubleValue() * investmentDTO.get().getQuantity());
            investmentDTO.get().setValueDiff(investmentDTO.get().getLiveValue() - investmentDTO.get().getAmount());
        }
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public void getStockWatchList(List<StockWatchDTO> stockWatchList) {
        log.trace("Enter getStockWatchList");
        String stockAPI = this.configRepository.findById("eod").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("eod").orElseThrow(NoSuchElementException::new).getSecret();
        String joinedList = stockWatchList.stream().map(s -> (s.getStockShortName() + "." + s.getStockExchange())).collect(Collectors.joining(","));
        String URL = MessageFormat.format(stockAPI + "/real-time/stock/?api_token={0}&fmt=json&s={1}", secret, joinedList);

        JsonNode responseBody = this.callStockAPI(URL);
        for (JsonNode stock : responseBody) {
            Optional<StockWatchDTO> stockWatchDTO = stockWatchList.stream().filter
                    (s -> (s.getStockShortName() + "." + s.getStockExchange()).equals(stock.findValue("code").textValue())).findFirst();
            if (stockWatchDTO.isEmpty()) continue;

            stockWatchDTO.get().setLiveValue(stock.findValue("close").doubleValue());
            stockWatchDTO.get().setChange(stock.findValue("change").doubleValue());
            stockWatchDTO.get().setPChange(stock.findValue("change_p").doubleValue());
            stockWatchDTO.get().setCurrencyId("USD");
        }
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public List<EODCandleQuote> getCandleQuoteByShortName(String shortname, int months) {
        log.trace("Enter getCandleQuoteByShortName");
        String stockAPI = this.configRepository.findById("eod").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("eod").orElseThrow(NoSuchElementException::new).getSecret();
        String from = (months <= 60) ? "&from=" + dateFormat.format(Date.from(ZonedDateTime.now().minusMonths(months).toInstant())) : "";
        String period = (months > 23) ? ((months > 60) ? "m" : "w") : "d";
        String URL = MessageFormat.format(stockAPI + "/eod/{0}?api_token={1}&fmt=json&period={2}{3}",
                shortname, secret, period, from);

        JsonNode root = this.callStockAPI(URL);
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readerFor(new TypeReference<List<EODCandleQuote>>() {
            }).readValue(root);
        } catch (NullPointerException | IOException e) {
            throw new APIException("JSON process failed. " + e.getMessage());
        }
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public List<Symbol> getAllSymbols(String exchange) {
        log.trace("Enter getAllSymbols");
        String stockAPI = this.configRepository.findById("eod").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("eod").orElseThrow(NoSuchElementException::new).getSecret();
        String URL = MessageFormat.format(stockAPI + "/exchange-symbol-list/{0}?api_token={1}&fmt=json", exchange, secret);

        JsonNode root = this.callStockAPI(URL);
        try {
            ObjectMapper mapper = new ObjectMapper();
            return Arrays.asList(mapper.treeToValue(root, Symbol[].class));
        } catch (JsonProcessingException | NullPointerException e) {
            throw new APIException("JSON process failed");
        }
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public List<Exchange> getAllExchanges() {
        log.trace("Enter getAllExchanges");
        String stockAPI = this.configRepository.findById("eod").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("eod").orElseThrow(NoSuchElementException::new).getSecret();
        String URL = MessageFormat.format(stockAPI + "/exchanges-list?api_token={0}&fmt=json", secret);

        JsonNode root = this.callStockAPI(URL);
        try {
            ObjectMapper mapper = new ObjectMapper();
            return Arrays.asList(mapper.treeToValue(root, Exchange[].class));
        } catch (JsonProcessingException | NullPointerException e) {
            throw new APIException("JSON process failed");
        }
    }

    private JsonNode callStockAPI(String URL) {
        log.trace("Enter callStockAPI");
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(URL, String.class);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(response.getBody());
        } catch (JsonProcessingException | NullPointerException e) {
            log.error("JSON process failed");
            throw new APIException("JSON process failed");
        } catch (RestClientException e) {
            log.error("Unable to reach stock API: " + e.getMessage());
            throw new APIException("Unable to reach stock API. " + e.getMessage());
        }
    }
}
