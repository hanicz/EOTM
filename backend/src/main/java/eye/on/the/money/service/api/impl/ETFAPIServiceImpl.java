package eye.on.the.money.service.api.impl;

import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.etf.ETF;
import eye.on.the.money.model.etf.ETFResponse;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import eye.on.the.money.service.api.ETFAPIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ETFAPIServiceImpl implements ETFAPIService {

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private ConfigRepository configRepository;

    @Override
    public void updateETFPrices(List<ETF> etfList) {
        String etfAPI = this.configRepository.findById("eod").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("eod").orElseThrow(NoSuchElementException::new).getSecret();
        String symbols = etfList.stream()
                .filter(etf -> this.isUpdateRequired(etf.getEodDate()))
                .map(etf -> etf.getShortName() + "." + etf.getExchange())
                .collect(Collectors.joining(","));

        if (symbols.isBlank() || symbols.isEmpty()) {
            return;
        }

        int symbolIndex = symbols.indexOf(",");
        String symbol = symbols;
        if (symbolIndex != -1) {
            symbol = symbol.substring(0, symbolIndex);
        }
        String URL = this.createURL(etfAPI, secret, symbol, symbols);
        Map<String, ETFResponse> etfMap = this.callETFAPI(URL);

        etfList.stream().filter(etf -> this.isUpdateRequired(etf.getEodDate())).forEach(etf -> {
            ETFResponse etfResponse = etfMap.get(etf.getShortName() + "." + etf.getExchange());
            etf.setLiveValue(etfResponse.getClose());
            etf.setEodDate(new Date());
        });
    }

    private boolean isUpdateRequired(Date lastUpdateDate) {
        Instant instant1 = lastUpdateDate.toInstant();
        Instant instant2 = new Date().toInstant();

        if (!instant1.truncatedTo(ChronoUnit.DAYS).equals(instant2.truncatedTo(ChronoUnit.DAYS))) {
            return true;
        }

        if (instant2.atZone(ZoneOffset.UTC).getHour() >= 18 && instant1.atZone(ZoneOffset.UTC).getHour() < 18) {
            return true;
        }

        return false;
    }

    @Retryable(value = APIException.class, maxAttempts = 3)
    private Map<String, ETFResponse> callETFAPI(String URL) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<ETFResponse[]> response = restTemplate.getForEntity(URL, ETFResponse[].class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, ETFResponse> etfMap = new HashMap<>();
            for (ETFResponse etf : response.getBody()) {
                etfMap.put(etf.getCode(), etf);
            }
            return etfMap;
        } else {
            throw new APIException("Unable to reach EOD API");
        }
    }

    private String createURL(String etfAPI, String secret, String symbol, String symbols) {
        return MessageFormat.format(
                etfAPI + "/real-time/{0}?&api_token={1}&s={2}&fmt=json",
                symbol, secret, symbols);
    }
}
