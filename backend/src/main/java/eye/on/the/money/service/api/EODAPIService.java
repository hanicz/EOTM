package eye.on.the.money.service.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.dto.in.EODCandleQuoteDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.model.stock.Symbol;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import eye.on.the.money.util.DateFormats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class EODAPIService extends APIService {

    private final static String API = "eod";

    private final static String EXCHANGE_SYMBOL_LIST_PATH = "/exchange-symbol-list/{1}?api_token={0}&fmt=json";
    private final static String EXCHANGE_LIST_PATH = "/exchanges-list?api_token={0}&fmt=json";
    private final static String CANDLE_PATH = "/eod/{1}?api_token={0}&fmt=json&period={2}{3}";
    private final static String SINGLE_TICKER_PATH = "/real-time/{1}/?api_token={0}&fmt=json";
    private final static String MULTIPLE_TICKER_PATH = SINGLE_TICKER_PATH + "&s={2}";

    @Autowired
    public EODAPIService(CredentialRepository credentialRepository, ConfigRepository configRepository,
                         WebClient webClient, ObjectMapper mapper) {
        super(credentialRepository, configRepository, webClient, mapper);
    }

    private JsonNode getLiveValue(String url) {
        log.trace("Enter");
        ResponseEntity<?> response = this.callGetAPI(url, String.class);
        return this.getJsonNodeFromBody((String) response.getBody());
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public JsonNode getLiveStockValue(String tickerList) {
        String url = this.createURL(EODAPIService.API, MULTIPLE_TICKER_PATH, "stock", tickerList);
        return this.getLiveValue(url);
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public JsonNode getLiveEtfValue(String tickerList) {
        String url = this.createURL(EODAPIService.API, MULTIPLE_TICKER_PATH, "etf", tickerList);
        return this.getLiveValue(url);
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public JsonNode getLiveForexValue(String tickerList) {
        String url = this.createURL(EODAPIService.API, MULTIPLE_TICKER_PATH, "forex", tickerList);
        return this.getLiveValue(url);
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public JsonNode getLiveValueForSingle(String ticker) {
        log.trace("Enter");
        String url = this.createURL(EODAPIService.API, SINGLE_TICKER_PATH, ticker);
        ResponseEntity<?> response = this.callGetAPI(url, String.class);
        return this.getJsonNodeFromBody((String) response.getBody());
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public List<EODCandleQuoteDTO> getCandleQuoteByShortName(String shortname, int months) {
        log.trace("Enter");
        String from = (months <= 60) ? "&from=" + LocalDate.now().minusMonths(months).format(DateFormats.YYYY_MM_DD) : "";
        String period = (months > 23) ? ((months > 60) ? "m" : "w") : "d";
        String url = this.createURL(EODAPIService.API, CANDLE_PATH, shortname, period, from);
        ResponseEntity<?> response = this.callGetAPI(url,
                EODCandleQuoteDTO[].class);
        return Arrays.asList((EODCandleQuoteDTO[]) response.getBody());
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public List<Symbol> getAllSymbols(String exchange) {
        log.trace("Enter");
        String url = this.createURL(EODAPIService.API, EXCHANGE_SYMBOL_LIST_PATH, exchange);
        ResponseEntity<?> response = this.callGetAPI(url,
                Symbol[].class);
        return Arrays.asList((Symbol[]) response.getBody());
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public List<Exchange> getAllExchanges() {
        log.trace("Enter");
        ResponseEntity<?> response = this.callGetAPI(this.createURL(EODAPIService.API, EXCHANGE_LIST_PATH),
                Exchange[].class);
        return Arrays.asList((Exchange[]) response.getBody());
    }
}
