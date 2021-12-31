package eye.on.the.money.service.currency.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.dto.out.CryptoWatchDTO;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.service.currency.CryptoAPIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class CryptoAPIServiceImpl implements CryptoAPIService {

    @Autowired
    private ConfigRepository configRepository;

    @Override
    public void getLiveValue(List<TransactionDTO> transactionDTOList, String currency){
        String cryptoAPI = this.configRepository.findById("coingecko").orElseThrow(NoSuchElementException::new).getConfigValue();
        String ids = transactionDTOList.stream().map(TransactionDTO::getCoinId).collect(Collectors.joining(","));
        String URL = this.createURL(cryptoAPI, ids, currency);
        JsonNode root = this.callCryptoAPI(URL);
        transactionDTOList.forEach(transactionDTO -> {
            transactionDTO.setLiveValue(root.path(transactionDTO.getCoinId()).get(currency.toLowerCase()).doubleValue() * transactionDTO.getQuantity());
        });
    }

    @Override
    public void getLiveValueForWatchList(List<CryptoWatchDTO> cryptoWatchDTOList, String currency){
        String cryptoAPI = this.configRepository.findById("coingecko").orElseThrow(NoSuchElementException::new).getConfigValue();
        String ids = cryptoWatchDTOList.stream().map(CryptoWatchDTO::getCoinId).collect(Collectors.joining(","));
        String URL = this.createURL(cryptoAPI, ids, currency);
        JsonNode root = this.callCryptoAPI(URL);
        cryptoWatchDTOList.forEach(cryptoWatchDTO -> {
            cryptoWatchDTO.setLiveValue(root.path(cryptoWatchDTO.getCoinId()).get(currency.toLowerCase()).doubleValue());
        });
    }

    @Retryable(value = APIException.class, maxAttempts = 3)
    private JsonNode callCryptoAPI(String URL) {
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(URL, String.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readTree(response.getBody());
            } catch (JsonProcessingException | NullPointerException e) {
                e.printStackTrace();
                throw new APIException("JSON process failed");
            }
        } else {
            throw new APIException("Unable to reach crypto API");
        }
    }

    private String createURL(String cryptoAPI, String symbol, String currency){
        return MessageFormat.format(
                cryptoAPI + "/simple/price?ids={0}&vs_currencies={1}",
                symbol, currency);
    }
}
