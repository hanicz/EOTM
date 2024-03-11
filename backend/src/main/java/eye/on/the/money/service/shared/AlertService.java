package eye.on.the.money.service.shared;

import eye.on.the.money.controller.CryptoAlertDTO;
import eye.on.the.money.dto.out.StockAlertDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.alert.CryptoAlert;
import eye.on.the.money.model.alert.StockAlert;
import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.repository.alert.CryptoAlertRepository;
import eye.on.the.money.repository.alert.StockAlertRepository;
import eye.on.the.money.service.crypto.CoinService;
import eye.on.the.money.service.stock.StockService;
import eye.on.the.money.service.user.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlertService {
    private final StockAlertRepository stockAlertRepository;
    private final CryptoAlertRepository cryptoAlertRepository;
    private final StockService stockService;
    private final CoinService coinService;
    private final ModelMapper modelMapper;
    private final UserServiceImpl userService;

    public List<StockAlertDTO> getAllStockAlerts(String userEmail) {
        List<StockAlert> stockAlerts = this.stockAlertRepository.findByUserEmailOrderByStockShortName(userEmail);
        return stockAlerts.stream().map(this::convertToStockAlertDTO).collect(Collectors.toList());
    }

    public List<CryptoAlertDTO> getAllCryptoAlerts(String userEmail) {
        List<CryptoAlert> cryptoAlerts = this.cryptoAlertRepository.findByUserEmailOrderByCoinSymbol(userEmail);
        return cryptoAlerts.stream().map(this::convertToCryptoAlertDTO).collect(Collectors.toList());
    }

    @Transactional
    public boolean deleteCryptoAlert(String userEmail, Long id) {
        return this.cryptoAlertRepository.deleteByIdAndUserEmail(id, userEmail) > 0;
    }

    @Transactional
    public boolean deleteStockAlert(String userEmail, Long id) {
        return this.stockAlertRepository.deleteByIdAndUserEmail(id, userEmail) > 0;
    }

    @Transactional
    public StockAlertDTO createNewStockAlert(UserDetails userDetails, StockAlertDTO stockAlertDTO) {
        Stock stock = this.stockService.getOrCreateStock(stockAlertDTO.getShortName(), stockAlertDTO.getExchange(), stockAlertDTO.getName());
        User user = this.userService.loadUserByEmail(userDetails.getUsername());

        StockAlert stockAlert = StockAlert.builder()
                .stock(stock)
                .user(user)
                .type(stockAlertDTO.getType())
                .valuePoint(stockAlertDTO.getValuePoint())
                .build();
        this.stockAlertRepository.save(stockAlert);
        log.debug("New stock alert created {}", stockAlert);
        return this.convertToStockAlertDTO(stockAlert);
    }

    @Transactional
    public CryptoAlertDTO createNewCryptoAlert(UserDetails userDetails, CryptoAlertDTO cryptoAlertDTO) {
        Coin coin = this.coinService.getCoinBySymbol(cryptoAlertDTO.getSymbol());
        User user = this.userService.loadUserByEmail(userDetails.getUsername());

        CryptoAlert cryptoAlert = CryptoAlert.builder()
                .coin(coin)
                .user(user)
                .type(cryptoAlertDTO.getType())
                .valuePoint(cryptoAlertDTO.getValuePoint())
                .build();
        this.cryptoAlertRepository.save(cryptoAlert);
        log.debug("New crypto alert created {}", cryptoAlert);
        return this.convertToCryptoAlertDTO(cryptoAlert);
    }

    private StockAlertDTO convertToStockAlertDTO(StockAlert stockAlert) {
        return this.modelMapper.map(stockAlert, StockAlertDTO.class);
    }

    private CryptoAlertDTO convertToCryptoAlertDTO(CryptoAlert cryptoAlert) {
        return this.modelMapper.map(cryptoAlert, CryptoAlertDTO.class);
    }
}
