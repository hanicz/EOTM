package eye.on.the.money.service.impl;

import eye.on.the.money.dto.out.StockAlertDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.alert.StockAlert;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.repository.alert.StockAlertRepository;
import eye.on.the.money.repository.stock.StockRepository;
import eye.on.the.money.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AlertServiceImpl implements AlertService {

    @Autowired
    private StockAlertRepository stockAlertRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public List<StockAlertDTO> getAllStockAlerts(Long userId) {
        log.trace("Enter");
        List<StockAlert> stockAlerts = this.stockAlertRepository.findByUser_IdOrderByStockShortName(userId);
        return stockAlerts.stream().map(this::convertToStockAlertDTO).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public boolean deleteStockAlert(Long userid, Long id) {
        log.trace("Enter");
        return this.stockAlertRepository.deleteByIdAndUser_Id(id, userid) > 0;
    }

    @Transactional
    @Override
    public StockAlertDTO createNewStockAlert(User user, StockAlertDTO stockAlertDTO) {
        log.trace("Enter");
        Stock stock = this.stockRepository.findById(stockAlertDTO.getShortName().toLowerCase()).orElseGet(() -> {
                    log.debug("Stock missing, creating new.");
                    Stock newStock = Stock.builder()
                            .id(stockAlertDTO.getShortName().toLowerCase())
                            .exchange(stockAlertDTO.getExchange())
                            .shortName(stockAlertDTO.getShortName().toUpperCase())
                            .name(stockAlertDTO.getName())
                            .build();
                    log.debug("New stock created: " + newStock.toString());
                    return this.stockRepository.save(newStock);
                }
        );

        StockAlert stockAlert = StockAlert.builder().stock(stock).user(user).type(stockAlertDTO.getType()).valuePoint(stockAlertDTO.getValuePoint()).build();
        this.stockAlertRepository.save(stockAlert);
        log.trace("New alert created " + stockAlert.toString());
        return this.convertToStockAlertDTO(stockAlert);
    }

    private StockAlertDTO convertToStockAlertDTO(StockAlert stockAlert) {
        log.trace("Enter");
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(stockAlert, StockAlertDTO.class);
    }
}
