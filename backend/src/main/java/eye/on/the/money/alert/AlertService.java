package eye.on.the.money.alert;

import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.model.alert.StockAlert;
import eye.on.the.money.repository.alert.StockAlertRepository;
import eye.on.the.money.service.api.EODAPIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AlertService {

    @Autowired
    private StockAlertRepository stockAlertRepository;

    @Autowired
    private EODAPIService eodAPIService;

    @Scheduled(fixedDelay = 100000)
    public void scheduleFixedDelayTask() {

        List<StockAlert> alertList = this.stockAlertRepository.findAll();

        List<InvestmentDTO> stocks = alertList.stream().map(StockAlert::getStock).collect(Collectors.toSet())
                .stream().map(s -> InvestmentDTO.builder().shortName(s.getShortName()).exchange(s.getExchange()).quantity(1).amount(1.0).build())
                .collect(Collectors.toList());

        this.eodAPIService.getLiveValue(stocks);

        for(StockAlert sa : alertList) {

        }
    }
}
