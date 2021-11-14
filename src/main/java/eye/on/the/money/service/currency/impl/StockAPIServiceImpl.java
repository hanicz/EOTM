package eye.on.the.money.service.currency.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.dto.out.InvestmentDTO;
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
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class StockAPIServiceImpl  implements StockAPIService {

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private ConfigRepository configRepository;

    @Override
    public void getLiveValue(List<InvestmentDTO> investmentDTOList){
        String stockAPI = this.configRepository.findById("alphavantage").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("alphavantage").orElseThrow(NoSuchElementException::new).getSecret();
        investmentDTOList.forEach(investmentDTO -> {
            String URL = this.createURL(stockAPI, secret, investmentDTO.getShortName());
            JsonNode liveValue = this.callStockAPI(URL);
            if(liveValue != null) {
                investmentDTO.setLiveValue(Double.parseDouble(liveValue.textValue()) * investmentDTO.getQuantity());
            }
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
                return root.findValue("05. price");
            } catch (JsonProcessingException | NullPointerException e) {
                e.printStackTrace();
                throw new APIException("JSON process failed");
            }
        } else {
            throw new APIException("Unable to reach currency API");
        }
    }

    private String createURL(String stockAPI, String secret, String symbol){
        return MessageFormat.format(
                stockAPI + "?function=GLOBAL_QUOTE&symbol={0}&apikey={1}",
                symbol, secret);
    }
}
