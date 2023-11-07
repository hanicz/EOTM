package eye.on.the.money.service.api.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.out.*;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EODAPIServiceImpl extends APIService implements EODAPIService {

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final static String API = "eod";

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public void getLiveValue(List<InvestmentDTO> investmentDTOList) {
        log.trace("Enter");
        String joinedList = investmentDTOList.stream().map(i -> (i.getShortName() + "." + i.getExchange())).collect(Collectors.joining(","));

        JsonNode responseBody = this.callStockAPI(this.createURL(EODAPIServiceImpl.API, "/real-time/stock/?api_token={0}&fmt=json&s={1}", joinedList));
        for (JsonNode stock : responseBody) {
            Optional<InvestmentDTO> investmentDTO = investmentDTOList.stream().filter
                    (i -> (i.getShortName() + "." + i.getExchange()).equals(stock.findValue("code").textValue())).findFirst();
            if (investmentDTO.isEmpty()) continue;
            investmentDTO.get().setLiveValue(stock.findValue("close").doubleValue() * investmentDTO.get().getQuantity());
            investmentDTO.get().setValueDiff(investmentDTO.get().getLiveValue() - investmentDTO.get().getAmount());
        }
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public void getETFLiveValue(List<ETFInvestmentDTO> investmentDTOList) {
        log.trace("Enter");
        String joinedList = investmentDTOList.stream().map(i -> (i.getShortName() + "." + i.getExchange())).collect(Collectors.joining(","));

        JsonNode responseBody = this.callStockAPI(this.createURL(EODAPIServiceImpl.API, "/real-time/etf/?api_token={0}&fmt=json&s={1}", joinedList));
        for (JsonNode etf : responseBody) {
            Optional<ETFInvestmentDTO> etfInvestmentDTO = investmentDTOList.stream().filter
                    (i -> (i.getShortName() + "." + i.getExchange()).equals(etf.findValue("code").textValue())).findFirst();
            if (etfInvestmentDTO.isEmpty()) continue;
            etfInvestmentDTO.get().setLiveValue(etf.findValue("close").doubleValue() * etfInvestmentDTO.get().getQuantity());
            etfInvestmentDTO.get().setValueDiff(etfInvestmentDTO.get().getLiveValue() - etfInvestmentDTO.get().getAmount());
        }
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public void getStockWatchList(List<StockWatchDTO> stockWatchList) {
        log.trace("Enter");
        String joinedList = stockWatchList.stream().map(s -> (s.getStockShortName() + "." + s.getStockExchange())).collect(Collectors.joining(","));

        JsonNode responseBody = this.callStockAPI(this.createURL(EODAPIServiceImpl.API, "/real-time/stock/?api_token={0}&fmt=json&s={1}", joinedList));
        for (JsonNode stock : responseBody) {
            Optional<StockWatchDTO> stockWatchDTO = stockWatchList.stream().filter
                    (s -> (s.getStockShortName() + "." + s.getStockExchange()).equals(stock.findValue("code").textValue())).findFirst();
            if (stockWatchDTO.isEmpty()) continue;

            stockWatchDTO.get().setLiveValue(stock.findValue("close").doubleValue());
            stockWatchDTO.get().setChange(stock.findValue("change").doubleValue());
            stockWatchDTO.get().setPChange(stock.findValue("change_p").doubleValue());
            stockWatchDTO.get().setCurrencyId("USD");
        }
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public void getForexWatchList(List<ForexWatchDTO> forexWatchList) {
        log.trace("Enter");
        String joinedList = forexWatchList.stream().map(f -> (f.getFromCurrencyId() + f.getToCurrencyId() + ".FOREX")).collect(Collectors.joining(","));

        JsonNode responseBody = this.callStockAPI(this.createURL(EODAPIServiceImpl.API, "/real-time/forex/?api_token={0}&fmt=json&s={1}", joinedList));
        for (JsonNode forex : responseBody) {
            Optional<ForexWatchDTO> forexWatchDTO = forexWatchList.stream().filter
                    (f -> (f.getFromCurrencyId() + f.getToCurrencyId() + ".FOREX").equals(forex.findValue("code").textValue())).findFirst();
            if (forexWatchDTO.isEmpty()) continue;
            forexWatchDTO.get().setLiveValue(forex.findValue("close").doubleValue());
            forexWatchDTO.get().setChange(forex.findValue("change").doubleValue() * -1);
            forexWatchDTO.get().setPChange(forex.findValue("change_p").doubleValue() * -1);
        }
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public void changeLiveValueCurrencyForForexTransactions(List<ForexTransactionDTO> forexTransactions) {
        log.trace("Enter");
        String joinedList = forexTransactions.stream().map(f -> (f.getToCurrencyId() + f.getFromCurrencyId() + ".FOREX")).collect(Collectors.joining(","));

        JsonNode responseBody = this.callStockAPI(this.createURL(EODAPIServiceImpl.API, "/real-time/forex/?api_token={0}&fmt=json&s={1}", joinedList));
        for (JsonNode forex : responseBody) {
            Optional<ForexTransactionDTO> forexTransactionDTO = forexTransactions.stream().filter
                    (f -> (f.getToCurrencyId() + f.getFromCurrencyId() + ".FOREX").equals(forex.findValue("code").textValue())).findFirst();
            if (forexTransactionDTO.isEmpty()) continue;
            forexTransactionDTO.get().setLiveValue(forex.findValue("close").doubleValue() * forexTransactionDTO.get().getToAmount());
            forexTransactionDTO.get().setLiveChangeRate(forex.findValue("close").doubleValue());
            forexTransactionDTO.get().setValueDiff(forexTransactionDTO.get().getLiveValue() - forexTransactionDTO.get().getFromAmount());
        }
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public List<EODCandleQuote> getCandleQuoteByShortName(String shortname, int months) {
        log.trace("Enter");
        String from = (months <= 60) ? "&from=" + this.dateFormat.format(Date.from(ZonedDateTime.now().minusMonths(months).toInstant())) : "";
        String period = (months > 23) ? ((months > 60) ? "m" : "w") : "d";
        JsonNode root = this.callStockAPI(this.createURL(EODAPIServiceImpl.API, "/eod/{1}?api_token={0}&fmt=json&period={2}{3}", shortname, period, from));
        try {
            return this.mapper.readerFor(new TypeReference<List<EODCandleQuote>>() {
            }).readValue(root);
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
            throw new APIException("JSON process failed");
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
            throw new APIException("JSON process failed");
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
            throw new APIException("Unable to reach stock API. " + e.getMessage());
        }
    }
}
