package eye.on.the.money.service.currency.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.dto.out.StockWatchDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import eye.on.the.money.service.currency.StockAPIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class StockAPIServiceImpl implements StockAPIService {

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private ConfigRepository configRepository;

    @Override
    public void getLiveValue(List<InvestmentDTO> investmentDTOList) {
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
    public void getStockWatchList(List<StockWatchDTO> stockWatchList) {
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

    @Retryable(value = APIException.class, maxAttempts = 3)
    private JsonNode callStockAPI(String URL) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(URL, String.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.getBody());
                return root;
            } catch (JsonProcessingException | NullPointerException e) {
                e.printStackTrace();
                throw new APIException("JSON process failed");
            }
        } else {
            throw new APIException("Unable to reach currency API");
        }
    }

    private String createURL(String stockAPI, String secret, String symbol) {
        return MessageFormat.format(
                stockAPI + "/quote?symbol={0}&token={1}",
                symbol, secret);
    }
}
