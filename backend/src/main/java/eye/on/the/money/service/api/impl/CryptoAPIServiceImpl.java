package eye.on.the.money.service.api.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.service.api.APIService;
import eye.on.the.money.service.api.CryptoAPIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
public class CryptoAPIServiceImpl extends APIService implements CryptoAPIService {

    private final static String API = "coingecko";

    @Retryable(value = APIException.class, maxAttempts = 3)
    public JsonNode getLiveValueForCoins(String currency, String ids) {
        log.trace("Enter");
        return this.callCryptoAPI(this.createURL(CryptoAPIServiceImpl.API,
                "/simple/price?ids={1}&vs_currencies={2}&include_24hr_change={3}&token={0}", ids, currency, Boolean.TRUE.toString()));
    }

    private JsonNode callCryptoAPI(String URL) {
        log.trace("Enter");
        try {
            log.debug("Call to {}", URL);
            ResponseEntity<String> response = this.restTemplate.getForEntity(URL, String.class);
            String responseBody = response.getBody();
            log.debug("Response: {}", responseBody);
            return this.mapper.readTree(responseBody);
        } catch (JsonProcessingException | NullPointerException e) {
            log.error("JSON process failed");
            throw new APIException("JSON process failed");
        } catch (RestClientException e) {
            log.error("Unable to reach crypto API: {}", e.getMessage());
            throw new APIException("Unable to reach crypto API");
        }
    }
}
