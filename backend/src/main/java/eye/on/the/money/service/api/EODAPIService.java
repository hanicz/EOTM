package eye.on.the.money.service.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.dto.in.EODCandleQuoteDTO;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.model.stock.Symbol;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class EODAPIService extends APIService {

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final static String API = "eod";

    @Autowired
    public EODAPIService(CredentialRepository credentialRepository, ConfigRepository configRepository,
                         WebClient webClient, ObjectMapper mapper) {
        super(credentialRepository, configRepository, webClient, mapper);
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public JsonNode getLiveValue(String tickerList, String path) {
        log.trace("Enter");
        String url = this.createURL(EODAPIService.API, path, tickerList);
        ResponseEntity<?> response = this.callGetAPI(url, String.class);
        return this.getJsonNodeFromBody((String) response.getBody());
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public JsonNode getLiveValueForSingle(String ticker, String path) {
        log.trace("Enter");
        String url = this.createURL(EODAPIService.API, path, ticker);
        ResponseEntity<?> response = this.callGetAPI(url, String.class);
        return this.getJsonNodeFromBody((String) response.getBody());
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public List<EODCandleQuoteDTO> getCandleQuoteByShortName(String shortname, int months) {
        log.trace("Enter");
        String from = (months <= 60) ? "&from=" + this.dateFormat.format(Date.from(ZonedDateTime.now().minusMonths(months).toInstant())) : "";
        String period = (months > 23) ? ((months > 60) ? "m" : "w") : "d";
        String url = this.createURL(EODAPIService.API, "/eod/{1}?api_token={0}&fmt=json&period={2}{3}", shortname, period, from);
        ResponseEntity<?> response = this.callGetAPI(url,
                EODCandleQuoteDTO[].class);
        return Arrays.asList((EODCandleQuoteDTO[]) response.getBody());
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public List<Symbol> getAllSymbols(String exchange) {
        log.trace("Enter");
        String url = this.createURL(EODAPIService.API, "/exchange-symbol-list/{1}?api_token={0}&fmt=json", exchange);
        ResponseEntity<?> response = this.callGetAPI(url,
                Symbol[].class);
        return Arrays.asList((Symbol[]) response.getBody());
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public List<Exchange> getAllExchanges() {
        log.trace("Enter");
        ResponseEntity<?> response = this.callGetAPI(this.createURL(EODAPIService.API, "/exchanges-list?api_token={0}&fmt=json"),
                Exchange[].class);
        return Arrays.asList((Exchange[]) response.getBody());
    }
}
