package eye.on.the.money.service.api.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.stock.EODCandleQuote;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.model.stock.Symbol;
import eye.on.the.money.service.api.APIService;
import eye.on.the.money.service.api.EODAPIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class EODAPIServiceImpl extends APIService implements EODAPIService {

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final static String API = "eod";

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public JsonNode getLiveValue(String tickerList, String path) {
        log.trace("Enter");
        return this.callStockAPI(this.createURL(EODAPIServiceImpl.API, path, tickerList));
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public JsonNode getLiveValueForSingle(String ticker, String path) {
        log.trace("Enter");
        return this.callStockAPI(this.createURL(EODAPIServiceImpl.API, path, ticker));
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public List<EODCandleQuote> getCandleQuoteByShortName(String shortname, int months) {
        log.trace("Enter");
        String from = (months <= 60) ? "&from=" + this.dateFormat.format(Date.from(ZonedDateTime.now().minusMonths(months).toInstant())) : "";
        String period = (months > 23) ? ((months > 60) ? "m" : "w") : "d";
        JsonNode root = this.callStockAPI(this.createURL(EODAPIServiceImpl.API, "/eod/{1}?api_token={0}&fmt=json&period={2}{3}", shortname, period, from));
        try {
            return Arrays.asList(this.mapper.treeToValue(root, EODCandleQuote[].class));
        } catch (NullPointerException | IOException e) {
            throw new APIException("JSON process failed. " + e.getMessage());
        }
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public List<Symbol> getAllSymbols(String exchange) {
        log.trace("Enter");
        JsonNode root = this.callStockAPI(this.createURL(EODAPIServiceImpl.API, "/exchange-symbol-list/{1}?api_token={0}&fmt=json", exchange));
        try {
            return Arrays.asList(this.mapper.treeToValue(root, Symbol[].class));
        } catch (JsonProcessingException | NullPointerException e) {
            throw new APIException("Symbol JSON process failed");
        }
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public List<Exchange> getAllExchanges() {
        log.trace("Enter");
        JsonNode root = this.callStockAPI(this.createURL(EODAPIServiceImpl.API,"/exchanges-list?api_token={0}&fmt=json"));
        try {
            return Arrays.asList(this.mapper.treeToValue(root, Exchange[].class));
        } catch (JsonProcessingException | NullPointerException e) {
            throw new APIException("Exchange JSON process failed");
        }
    }

    private JsonNode callStockAPI(String URL) {
        log.trace("Enter");
        try {
            ResponseEntity<String> response = this.restTemplate.getForEntity(URL, String.class);
            return this.mapper.readTree(response.getBody());
        } catch (JsonProcessingException | NullPointerException e) {
            log.error("JSON process failed");
            throw new APIException("JSON process failed");
        } catch (RestClientException e) {
            log.error("Unable to reach stock API: " + e.getMessage());
            throw new APIException("Unable to reach stock API.");
        }
    }
}
