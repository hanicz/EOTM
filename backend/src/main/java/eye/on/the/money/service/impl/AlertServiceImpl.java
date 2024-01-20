package eye.on.the.money.service.impl;

import eye.on.the.money.dto.out.StockAlertDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.alert.StockAlert;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.repository.alert.StockAlertRepository;
import eye.on.the.money.repository.stock.StockRepository;
import eye.on.the.money.service.AlertService;
import eye.on.the.money.service.stock.StockService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
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
    private StockService stockService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private UserServiceImpl userService;

    @Override
    public List<StockAlertDTO> getAllStockAlerts(String userEmail) {
        log.trace("Enter");
        List<StockAlert> stockAlerts = this.stockAlertRepository.findByUserEmailOrderByStockShortName(userEmail);
        return stockAlerts.stream().map(this::convertToStockAlertDTO).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public boolean deleteStockAlert(String userEmail, Long id) {
        log.trace("Enter");
        return this.stockAlertRepository.deleteByIdAndUserEmail(id, userEmail) > 0;
    }

    @Transactional
    @Override
    public StockAlertDTO createNewStockAlert(UserDetails userDetails, StockAlertDTO stockAlertDTO) {
        log.trace("Enter");
        Stock stock = this.stockService.getOrCreateStock(stockAlertDTO.getShortName(), stockAlertDTO.getExchange(), stockAlertDTO.getName());
        User user = this.userService.loadUserByEmail(userDetails.getUsername());

        StockAlert stockAlert = StockAlert.builder().stock(stock).user(user).type(stockAlertDTO.getType()).valuePoint(stockAlertDTO.getValuePoint()).build();
        this.stockAlertRepository.save(stockAlert);
        log.debug("New alert created {}", stockAlert);
        return this.convertToStockAlertDTO(stockAlert);
    }

    private StockAlertDTO convertToStockAlertDTO(StockAlert stockAlert) {
        log.trace("Enter");
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(stockAlert, StockAlertDTO.class);
    }
}
