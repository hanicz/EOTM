package eye.on.the.money.service.api.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.dto.out.StockWatchDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.stock.CandleQuote;
import eye.on.the.money.model.stock.Symbol;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import eye.on.the.money.service.api.StockAPIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;
import java.util.*;

@Service
@Slf4j
public class StockAPIServiceImpl implements StockAPIService {

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private ConfigRepository configRepository;

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public void getLiveValue(List<InvestmentDTO> investmentDTOList) {
        log.trace("Enter getLiveValue");
        String stockAPI = this.configRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getSecret();
        investmentDTOList.stream().filter(i -> i.getCurrencyId().equals("USD")).forEach(investmentDTO -> {
            String URL = this.createURL(stockAPI, secret, investmentDTO.getShortName());
            JsonNode liveValue = this.callStockAPI(URL).findValue("c");
            if (liveValue != null) {
                investmentDTO.setLiveValue(liveValue.doubleValue() * investmentDTO.getQuantity());
            }
        });
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public void getStockWatchList(List<StockWatchDTO> stockWatchList) {
        log.trace("Enter getStockWatchList");
        String stockAPI = this.configRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getSecret();
        stockWatchList.forEach(stockWatchDTO -> {
            String URL = this.createURL(stockAPI, secret, stockWatchDTO.getStockShortName());
            JsonNode liveValue = this.callStockAPI(URL);
            stockWatchDTO.setLiveValue(liveValue.findValue("c").doubleValue());
            stockWatchDTO.setChange(liveValue.findValue("d").doubleValue());
            stockWatchDTO.setPChange(liveValue.findValue("dp").doubleValue());
            stockWatchDTO.setCurrencyId("USD");
        });
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public CandleQuote getCandleQuoteByShortName(String shortname, int months) {
        log.trace("Enter getCandleQuoteByShortName");
        String stockAPI = this.configRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getSecret();
        Long monthEpoch = new Date(System.currentTimeMillis() - months * 30L * 24 * 60 * 60 * 1000).getTime() / 1000;
        String URL = MessageFormat.format(stockAPI + "/stock/candle?symbol={0}&resolution=D&from={1}&to={2}&token={3}",
                shortname, String.valueOf(monthEpoch), String.valueOf(new Date().getTime() / 1000), secret);
        JsonNode root = this.callStockAPI(URL);
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.treeToValue(root, CandleQuote.class);
        } catch (JsonProcessingException | NullPointerException e) {
            throw new APIException("JSON process failed");
        }
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public List<Symbol> getAllSymbols() {
        log.trace("Enter getAllSymbols");
        String stockAPI = this.configRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getSecret();
        String URL = MessageFormat.format(stockAPI + "/stock/symbol?exchange=US&token={0}", secret);
        JsonNode root = this.callStockAPI(URL);
        try {
            ObjectMapper mapper = new ObjectMapper();
            return Arrays.asList(mapper.treeToValue(root, Symbol[].class));
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
            log.error("Unable to reach currency API: " + e.getMessage());
            throw new APIException("Unable to reach currency API");
        }
    }

    private String createURL(String stockAPI, String secret, String symbol) {
        return MessageFormat.format(
                stockAPI + "/quote?symbol={0}&token={1}",
                symbol, secret);
    }
}
