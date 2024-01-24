package eye.on.the.money.alert;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.model.alert.Alert;
import eye.on.the.money.model.alert.CryptoAlert;
import eye.on.the.money.model.alert.StockAlert;
import eye.on.the.money.repository.alert.CryptoAlertRepository;
import eye.on.the.money.repository.alert.StockAlertRepository;
import eye.on.the.money.service.api.CryptoAPIService;
import eye.on.the.money.service.api.EODAPIService;
import eye.on.the.money.service.mail.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
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

    private final static Map<String, String> messageMap = new HashMap<>();

    static {
        AlertScheduler.messageMap.put("PERCENT_OVER", "{0} is over {1}%");
        AlertScheduler.messageMap.put("PERCENT_UNDER", "{0} is under {1}%");
        AlertScheduler.messageMap.put("PRICE_OVER", "{0} is over {1} price point");
        AlertScheduler.messageMap.put("PRICE_UNDER", "{0} is under {1} price point");
    }


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
        String ids = String.join(",", cryptoAlerts.stream()
                .map(alert -> alert.getCoin().getId())
                .collect(Collectors.toSet()));

        JsonNode responseBody = this.cryptoAPIService.getLiveValueForCoins("eur", ids);
        Iterator<Map.Entry<String, JsonNode>> fields = responseBody.fields();
        Map<String, JsonNode> coinMap = new HashMap<>();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            coinMap.put(field.getKey(), field.getValue());
        }

        cryptoAlerts.parallelStream().forEach(alert -> {
            alert.setActualChange(coinMap.get(alert.getCoin().getId()).findValue("eur_24h_change").asDouble());
            alert.setActualValue(coinMap.get(alert.getCoin().getId()).findValue("eur").asDouble());
            alert.setSymbolOrTicker(alert.getCoin().getId());
            this.evaluateAlert(alert);
        });
        log.trace("Exit");
    }

    private void checkStockAlerts() {
        log.trace("Enter");
        List<StockAlert> stockAlertList = this.stockAlertRepository.findAll();
        if (stockAlertList.isEmpty()) return;
        String joinedList = String.join(",", stockAlertList.stream()
                .map(alert -> alert.getStock().getShortName() + "." + alert.getStock().getExchange())
                .collect(Collectors.toSet()));

        JsonNode responseBody = this.eodAPIService.getLiveValue(joinedList, "/real-time/stock/?api_token={0}&fmt=json&s={1}");
        Map<String, JsonNode> stockMap = new HashMap<>();
        for (JsonNode stock : responseBody) {
            stockMap.put(stock.findValue("code").textValue(), stock);
        }

        stockAlertList.parallelStream().forEach(alert -> {
            String ticker = alert.getStock().getShortName() + "." + alert.getStock().getExchange();
            alert.setSymbolOrTicker(ticker);
            alert.setActualChange(stockMap.get(ticker).findValue("change_p").asDouble());
            alert.setActualValue(stockMap.get(ticker).findValue("close").asDouble());
            this.evaluateAlert(alert);
        });
        log.trace("Exit");
    }

    private void evaluateAlert(Alert alert) {
        log.trace("Enter");

        log.trace("Checking alert: {}", alert);
        if (alert.isAlertActive()) {
            String message = MessageFormat.format(AlertScheduler.messageMap.get(alert.getType()), alert.getSymbolOrTicker(), Double.toString(alert.getValuePoint()));
            this.sendAndDelete(alert, message);
        }
        log.trace("Alert checked with id: {}", alert.getId());
        log.trace("Exit");
    }

    private void sendAndDelete(Alert alert, String message) {
        this.emailServiceImpl.sendMail(alert.getUser().getEmail(), message);
        if ("stock".equals(alert.getAlertType()))
            this.stockAlertRepository.deleteById(alert.getId());
        else
            this.cryptoAlertRepository.deleteById(alert.getId());
    }
}
