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

    private final static String CRYPTO_PATH = "/simple/price?ids={0}&vs_currencies={1}&include_24hr_change={2}";

    @Autowired
    public CryptoAPIService(CredentialRepository credentialRepository, ConfigRepository configRepository,
                            WebClient webClient, ObjectMapper mapper) {
        super(credentialRepository, configRepository, webClient, mapper);
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public JsonNode getLiveValueForCoins(String currency, String ids) {
        log.trace("Enter");
        String url = this.getApiUrl(CryptoAPIService.API) + this.expandTemplate(CryptoAPIService.CRYPTO_PATH,
                new Object[]{this.encodeQuery(ids), this.encodeQuery(currency), Boolean.TRUE.toString()});
        ResponseEntity<?> response = this.callGetAPI(url, String.class);
        return this.getJsonNodeFromBody((String) response.getBody());
    }
}
