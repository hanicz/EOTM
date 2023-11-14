package eye.on.the.money.alert;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.mail.impl.EmailServiceImpl;
import eye.on.the.money.model.alert.StockAlert;
import eye.on.the.money.repository.alert.StockAlertRepository;
import eye.on.the.money.service.api.EODAPIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AlertScheduler {

    @Autowired
    private StockAlertRepository stockAlertRepository;

    @Autowired
    private EODAPIService eodAPIService;

    @Autowired
    private EmailServiceImpl emailServiceImpl;

    @Scheduled(fixedDelay = 300000)
    public void checkAlerts() {
        log.trace("Enter");
        List<StockAlert> alertList = this.stockAlertRepository.findAll();
        if (alertList.isEmpty()) return;
        String joinedList = alertList.stream().map(StockAlert::getStock).collect(Collectors.toSet())
                .stream().map(s -> (s.getShortName() + "." + s.getExchange())).collect(Collectors.joining(","));

        JsonNode responseBody = this.eodAPIService.getLiveValue(joinedList, "/real-time/stock/?api_token={0}&fmt=json&s={1}");
        Map<String, JsonNode> stockMap = new HashMap<>();

        for (JsonNode stock : responseBody) {
            stockMap.put(stock.findValue("code").textValue(), stock);
        }
        this.sendAlerts(alertList, stockMap);
    }

    private void sendAlerts(List<StockAlert> alertList, Map<String, JsonNode> stockMap) {
        for (StockAlert alert : alertList) {
            String ticker = alert.getStock().getShortName() + "." + alert.getStock().getExchange();
            switch (alert.getType()) {
                case "PERCENT_OVER":
                    if (stockMap.get(ticker).findValue("change").asDouble() >= alert.getValuePoint()) {
                        this.emailServiceImpl.sendMail(alert.getUser().getEmail(), ticker + " is over " + alert.getValuePoint() + "%");
                        this.stockAlertRepository.delete(alert);
                    }
                    break;
                case "PERCENT_UNDER":
                    if (stockMap.get(ticker).findValue("change").asDouble() <= alert.getValuePoint()) {
                        this.emailServiceImpl.sendMail(alert.getUser().getEmail(), ticker + " is under " + alert.getValuePoint() + "%");
                        this.stockAlertRepository.delete(alert);
                    }
                    break;
                case "PRICE_OVER":
                    if (stockMap.get(ticker).findValue("close").asDouble() >= alert.getValuePoint()) {
                        this.emailServiceImpl.sendMail(alert.getUser().getEmail(), ticker + " is over " + alert.getValuePoint() + "price point");
                        this.stockAlertRepository.delete(alert);
                    }
                    break;
                case "PRICE_UNDER":
                    if (stockMap.get(ticker).findValue("close").asDouble() <= alert.getValuePoint()) {
                        this.emailServiceImpl.sendMail(alert.getUser().getEmail(), ticker + " is under " + alert.getValuePoint() + "price point");
                        this.stockAlertRepository.delete(alert);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
