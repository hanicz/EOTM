package eye.on.the.money.alert;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.mail.EmailService;
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
        List<StockAlert> stockAlertList = this.stockAlertRepository.findAll();
        if (stockAlertList.isEmpty()) return;
        String joinedList = stockAlertList.stream().map(StockAlert::getStock).collect(Collectors.toSet())
                .stream().map(s -> (s.getShortName() + "." + s.getExchange())).collect(Collectors.joining(","));

        JsonNode responseBody = this.eodAPIService.getLiveValue(joinedList, "/real-time/stock/?api_token={0}&fmt=json&s={1}");
        Map<String, JsonNode> stockMap = new HashMap<>();

        for (JsonNode stock : responseBody) {
            stockMap.put(stock.findValue("code").textValue(), stock);
        }
        this.sendAlerts(stockAlertList, stockMap);
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


        while(fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            coinMap.put(field.getKey(), field.getValue());
        }

    }

    private void checkStockAlerts() {
        List<StockAlert> stockAlertList = this.stockAlertRepository.findAll();
        if (stockAlertList.isEmpty()) return;
        String joinedList = stockAlertList.stream().map(StockAlert::getStock).collect(Collectors.toSet())
                .stream().map(s -> (s.getShortName() + "." + s.getExchange())).collect(Collectors.joining(","));

        JsonNode responseBody = this.eodAPIService.getLiveValue(joinedList, "/real-time/stock/?api_token={0}&fmt=json&s={1}");
        Map<String, JsonNode> stockMap = new HashMap<>();

        for (JsonNode stock : responseBody) {
            stockMap.put(stock.findValue("code").textValue(), stock);
        }
        this.sendAlerts(stockAlertList, stockMap);
    }

    private void sendAlerts(List<StockAlert> alertList, Map<String, JsonNode> stockMap) {
        log.trace("Enter");
        for (StockAlert alert : alertList) {
            String ticker = alert.getStock().getShortName() + "." + alert.getStock().getExchange();
            switch (alert.getType()) {
                case "PERCENT_OVER":
                    if (stockMap.get(ticker).findValue("change_p").asDouble() >= alert.getValuePoint()) {
                        this.emailServiceImpl.sendMail(alert.getUser().getEmail(), ticker + " is over " + alert.getValuePoint() + "%");
                        this.stockAlertRepository.delete(alert);
                    }
                    break;
                case "PERCENT_UNDER":
                    if (stockMap.get(ticker).findValue("change_p").asDouble() <= alert.getValuePoint()) {
                        this.emailServiceImpl.sendMail(alert.getUser().getEmail(), ticker + " is under " + alert.getValuePoint() + "%");
                        this.stockAlertRepository.delete(alert);
                    }
                    break;
                case "PRICE_OVER":
                    if (stockMap.get(ticker).findValue("close").asDouble() >= alert.getValuePoint()) {
                        this.emailServiceImpl.sendMail(alert.getUser().getEmail(), ticker + " is over " + alert.getValuePoint() + " price point");
                        this.stockAlertRepository.delete(alert);
                    }
                    break;
                case "PRICE_UNDER":
                    if (stockMap.get(ticker).findValue("close").asDouble() <= alert.getValuePoint()) {
                        this.emailServiceImpl.sendMail(alert.getUser().getEmail(), ticker + " is under " + alert.getValuePoint() + " price point");
                        this.stockAlertRepository.delete(alert);
                    }
                    break;
                default:
                    break;
            }
        }
        log.trace("Exit");
    }
}
