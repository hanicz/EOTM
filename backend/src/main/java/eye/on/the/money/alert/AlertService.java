package eye.on.the.money.alert;

import eye.on.the.money.model.alert.StockAlert;
import eye.on.the.money.repository.alert.StockAlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AlertService {

    @Autowired
    private StockAlertRepository stockAlertRepository;

    @Scheduled(fixedDelay = 100000)
    public void scheduleFixedDelayTask() {

        Iterable<StockAlert> alertList = this.stockAlertRepository.findAll();

        for(StockAlert sa : alertList) {
            log.trace(sa.toString());
        }
    }
}
