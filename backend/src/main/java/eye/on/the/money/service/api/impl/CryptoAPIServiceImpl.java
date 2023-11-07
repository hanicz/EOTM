package eye.on.the.money.service.api.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.out.CryptoWatchDTO;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.service.api.APIService;
import eye.on.the.money.service.api.CryptoAPIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CryptoAPIServiceImpl extends APIService implements CryptoAPIService {

    private final static String API = "coingecko";

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public void getLiveValue(List<TransactionDTO> transactionDTOList, String currency) {
        log.trace("Enter");
        String ids = transactionDTOList.stream().map(TransactionDTO::getCoinId).collect(Collectors.joining(","));
        JsonNode root = this.callCryptoAPI(this.createURL(CryptoAPIServiceImpl.API,
                "/simple/price?ids={1}&vs_currencies={2}&include_24hr_change={3}&token={0}", ids, currency, Boolean.FALSE.toString()));

        transactionDTOList.forEach(transactionDTO -> {
            transactionDTO.setLiveValue(root.path(transactionDTO.getCoinId()).get(currency.toLowerCase()).doubleValue() * transactionDTO.getQuantity());
            transactionDTO.setValueDiff(transactionDTO.getLiveValue() - transactionDTO.getAmount());
        });
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public void getLiveValueForWatchList(List<CryptoWatchDTO> cryptoWatchDTOList, String currency) {
        log.trace("Enter");
        String ids = cryptoWatchDTOList.stream().map(CryptoWatchDTO::getCoinId).collect(Collectors.joining(","));
        JsonNode root = this.callCryptoAPI(this.createURL(CryptoAPIServiceImpl.API,
                "/simple/price?ids={1}&vs_currencies={2}&include_24hr_change={3}&token={0}", ids, currency, Boolean.TRUE.toString()));

        cryptoWatchDTOList.forEach(cryptoWatchDTO -> {
            cryptoWatchDTO.setLiveValue(root.path(cryptoWatchDTO.getCoinId()).get(currency.toLowerCase()).doubleValue());
            cryptoWatchDTO.setChange(root.path(cryptoWatchDTO.getCoinId()).get(currency.toLowerCase() + "_24h_change").doubleValue());
        });
    }

    private JsonNode callCryptoAPI(String URL) {
        log.trace("Enter");
        try {
            ResponseEntity<String> response = this.restTemplate.getForEntity(URL, String.class);
            return this.mapper.readTree(response.getBody());
        } catch (JsonProcessingException | NullPointerException e) {
            log.error("JSON process failed");
            throw new APIException("JSON process failed");
        } catch (RestClientException e) {
            log.error("Unable to reach crypto API: " + e.getMessage());
            throw new APIException("Unable to reach crypto API");
        }
    }
}
