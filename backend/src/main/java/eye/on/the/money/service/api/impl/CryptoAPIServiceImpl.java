package eye.on.the.money.service.api.impl;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.service.api.APIService;
import eye.on.the.money.service.api.CryptoAPIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CryptoAPIServiceImpl extends APIService implements CryptoAPIService {

    private final static String API = "coingecko";

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public JsonNode getLiveValueForCoins(String currency, String ids) {
        log.trace("Enter");
        ResponseEntity<?> response = this.callGetAPI(this.createURL(CryptoAPIServiceImpl.API,
                "/simple/price?ids={1}&vs_currencies={2}&include_24hr_change={3}&token={0}", ids, currency, Boolean.TRUE.toString()), String.class);
        return this.getJsonNodeFromBody((String) response.getBody());
    }
}
