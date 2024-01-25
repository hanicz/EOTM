package eye.on.the.money.service.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class CryptoAPIService extends APIService {

    private final static String API = "coingecko";

    @Autowired
    public CryptoAPIService(CredentialRepository credentialRepository, ConfigRepository configRepository,
                            WebClient webClient, ObjectMapper mapper) {
        super(credentialRepository, configRepository, webClient, mapper);
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public JsonNode getLiveValueForCoins(String currency, String ids) {
        log.trace("Enter");
        ResponseEntity<?> response = this.callGetAPI(this.createURL(CryptoAPIService.API,
                "/simple/price?ids={1}&vs_currencies={2}&include_24hr_change={3}&token={0}", ids, currency, Boolean.TRUE.toString()), String.class);
        return this.getJsonNodeFromBody((String) response.getBody());
    }
}
