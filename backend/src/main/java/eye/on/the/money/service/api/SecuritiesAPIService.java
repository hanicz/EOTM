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

import java.time.LocalDate;

@Service
@Slf4j
public class SecuritiesAPIService extends APIService {

    private static final String API = "securities";

    private static final String ACTUAL_INTERESTS = "?query=getActualInterests&date={0}";

    private static final String SECURITIES = "?query=getSecurityWithOutstanding";

    private static final String STATUS_OK = "OK";

    @Autowired
    public SecuritiesAPIService(CredentialRepository credentialRepository, ConfigRepository configRepository,
                                WebClient webClient, ObjectMapper mapper) {
        super(credentialRepository, configRepository, webClient, mapper);
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public JsonNode getActualInterests(LocalDate date) {
        log.trace("Enter getActualInterests");
        return this.query(this.expandTemplate(SecuritiesAPIService.ACTUAL_INTERESTS, new Object[]{date.toString()}));
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public JsonNode getSecurities() {
        log.trace("Enter getSecurities");
        return this.query(SecuritiesAPIService.SECURITIES);
    }

    private JsonNode query(String queryString) {
        ResponseEntity<?> response =
                this.callGetAPI(this.getApiUrl(SecuritiesAPIService.API) + queryString, String.class);
        JsonNode body = this.getJsonNodeFromBody((String) response.getBody());
        String status = body.path("meta").path("status").asText();
        if (!SecuritiesAPIService.STATUS_OK.equals(status)) {
            log.error("Securities query failed with status {}: {}", status, body.path("meta").path("message").asText());
            throw new APIException("Securities query failed with status " + status);
        }
        return body.path("data");
    }
}
