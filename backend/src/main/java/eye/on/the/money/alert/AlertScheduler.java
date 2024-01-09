package eye.on.the.money.alert;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.mail.EmailService;
import eye.on.the.money.model.alert.CommonAlert;
import eye.on.the.money.model.alert.CryptoAlert;
import eye.on.the.money.model.alert.StockAlert;
import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.repository.alert.CryptoAlertRepository;
import eye.on.the.money.repository.alert.StockAlertRepository;
import eye.on.the.money.service.api.CryptoAPIService;
import eye.on.the.money.service.api.EODAPIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AlertScheduler {

    @Autowired
    private StockAlertRepository stockAlertRepository;
    @Autowired
    private CryptoAlertRepository cryptoAlertRepository;
    @Autowired
    private EODAPIService eodAPIService;
    @Autowired
    private CryptoAPIService cryptoAPIService;
    @Autowired
    private EmailService emailServiceImpl;


    @Scheduled(fixedDelay = 300000)
    public void checkAlerts() {
        log.trace("Enter");
        this.checkStockAlerts();
        this.checkCryptoAlerts();
        log.trace("Exit");
    }

    private void checkCryptoAlerts() {
        log.trace("Enter");
        List<CryptoAlert> cryptoAlerts = this.cryptoAlertRepository.findAll();
        if (cryptoAlerts.isEmpty()) return;
        String ids = cryptoAlerts.stream().map(CryptoAlert::getCoin).collect(Collectors.toSet())
                .stream().map(Coin::getId).collect(Collectors.joining(","));

        JsonNode responseBody = this.cryptoAPIService.getLiveValueForCoins("eur", ids);
        Iterator<Map.Entry<String, JsonNode>> fields = responseBody.fields();
        Map<String, JsonNode> coinMap = new HashMap<>();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            coinMap.put(field.getKey(), field.getValue());
        }

        List<CommonAlert> commonAlerts = new ArrayList<>();

        cryptoAlerts.parallelStream().forEach(alert -> {
            commonAlerts.add(CommonAlert.builder()
                    .id(alert.getId())
                    .type(alert.getType())
                    .valuePoint(alert.getValuePoint())
                    .user(alert.getUser())
                    .alertType("crypto")
                    .symbolOrTicker(alert.getCoin().getId())
                    .actualValue(coinMap.get(alert.getCoin().getId()).findValue("eur").asDouble())
                    .actualChange(coinMap.get(alert.getCoin().getId()).findValue("eur_24h_change").asDouble())
                    .build());
        });

        this.sendAlerts(commonAlerts);
        log.trace("Exit");
    }

    private void checkStockAlerts() {
        log.trace("Enter");
        List<StockAlert> stockAlertList = this.stockAlertRepository.findAll();
        if (stockAlertList.isEmpty()) return;
        String joinedList = stockAlertList.stream().map(StockAlert::getStock).collect(Collectors.toSet())
                .stream().map(s -> (s.getShortName() + "." + s.getExchange())).collect(Collectors.joining(","));

        JsonNode responseBody = this.eodAPIService.getLiveValue(joinedList, "/real-time/stock/?api_token={0}&fmt=json&s={1}");
        Map<String, JsonNode> stockMap = new HashMap<>();
        for (JsonNode stock : responseBody) {
            stockMap.put(stock.findValue("code").textValue(), stock);
        }

        List<CommonAlert> commonAlerts = new ArrayList<>();

        stockAlertList.parallelStream().forEach(alert -> {
            String ticker = alert.getStock().getShortName() + "." + alert.getStock().getExchange();
            commonAlerts.add(CommonAlert.builder()
                    .id(alert.getId())
                    .type(alert.getType())
                    .valuePoint(alert.getValuePoint())
                    .user(alert.getUser())
                    .symbolOrTicker(ticker)
                    .alertType("stock")
                    .actualChange(stockMap.get(ticker).findValue("change_p").asDouble())
                    .actualValue(stockMap.get(ticker).findValue("close").asDouble())
                    .build());
        });

        this.sendAlerts(commonAlerts);
        log.trace("Exit");
    }

    private void sendAlerts(List<CommonAlert> alertList) {
        log.trace("Enter");
        alertList.parallelStream().forEach(alert -> {
            log.trace("Checking alert: {}", alert);
            switch (alert.getType()) {
                case "PERCENT_OVER":
                    if (alert.getActualChange() >= alert.getValuePoint()) {
                        this.sendAndDelete(alert, alert.getSymbolOrTicker() + " is over " + alert.getValuePoint() + "%");
                    }
                    break;
                case "PERCENT_UNDER":
                    if (alert.getActualChange() <= alert.getValuePoint()) {
                        this.sendAndDelete(alert, alert.getSymbolOrTicker() + " is under " + alert.getValuePoint() + "%");
                    }
                    break;
                case "PRICE_OVER":
                    if (alert.getActualValue() >= alert.getValuePoint()) {
                        this.sendAndDelete(alert, alert.getSymbolOrTicker() + " is over " + alert.getValuePoint() + " price point");
                    }
                    break;
                case "PRICE_UNDER":
                    if (alert.getActualValue() <= alert.getValuePoint()) {
                        this.sendAndDelete(alert, alert.getSymbolOrTicker() + " is under " + alert.getValuePoint() + " price point");
                    }
                    break;
                default:
                    break;
            }
            log.trace("Alert checked with id: {}", alert.getId());
        });
        log.trace("Exit");
    }

    private void sendAndDelete(CommonAlert alert, String message) {
        this.emailServiceImpl.sendMail(alert.getUser().getEmail(), message);
        if ("stock".equals(alert.getAlertType()))
            this.stockAlertRepository.deleteById(alert.getId());
        else
            this.cryptoAlertRepository.deleteById(alert.getId());
    }
}
