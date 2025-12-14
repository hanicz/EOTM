package eye.on.the.money.service.shared;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.CryptoAlertDTO;
import eye.on.the.money.dto.out.StockAlertDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.alert.CryptoAlert;
import eye.on.the.money.model.alert.StockAlert;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.repository.alert.CryptoAlertRepository;
import eye.on.the.money.repository.alert.StockAlertRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
public class AlertServiceTest {

    @Autowired
    private StockAlertRepository stockAlertRepository;

    @Autowired
    private CryptoAlertRepository cryptoAlertRepository;

    @Autowired
    private AlertService alertService;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @Autowired
    private ModelMapper modelMapper;

    @BeforeEach
    public void init() {
        this.user = this.userRepository.findByEmail("test@test.test");
    }

    @Test
    public void getAllStockAlerts() {
        List<StockAlert> alertsInDB = this.stockAlertRepository.findByUserEmailOrderByStockShortName(this.user.getUsername());
        List<StockAlertDTO> alertsResponse = this.alertService.getAllStockAlerts(this.user.getUsername());

        Assertions.assertEquals(alertsInDB.stream().map(this::convertToStockAlertDTO).collect(Collectors.toList()), alertsResponse);
    }

    @Test
    public void getAllCryptoAlerts() {
        List<CryptoAlert> alertsInDB = this.cryptoAlertRepository.findByUserEmailOrderByCoinSymbol(this.user.getUsername());
        List<CryptoAlertDTO> alertsResponse = this.alertService.getAllCryptoAlerts(this.user.getUsername());

        Assertions.assertEquals(alertsInDB.stream().map(this::convertToCryptoAlertDTO).collect(Collectors.toList()), alertsResponse);
    }

    @Test
    public void createNewStockAlert() {
        StockAlertDTO sDTO = this.getStockAlertDTO();
        StockAlertDTO createdDTO = this.alertService.createNewStockAlert(this.user, sDTO);
        sDTO.setId(createdDTO.getId());
        StockAlert expected = this.stockAlertRepository.findById(createdDTO.getId()).get();

        Assertions.assertEquals(this.convertToStockAlertDTO(expected), sDTO);
    }

    @Test
    public void createNewCryptoAlert() {
        CryptoAlertDTO cDTO = this.getCryptoAlertDTO();
        CryptoAlertDTO createdDTO = this.alertService.createNewCryptoAlert(this.user, cDTO);
        cDTO.setId(createdDTO.getId());
        CryptoAlert expected = this.cryptoAlertRepository.findById(createdDTO.getId()).get();

        Assertions.assertEquals(this.convertToCryptoAlertDTO(expected), cDTO);
    }

    @Test
    public void deleteStockAlert() {
        StockAlertDTO sDTO = this.getStockAlertDTO();
        StockAlertDTO createdDTO = this.alertService.createNewStockAlert(this.user, sDTO);
        Optional<StockAlert> beforeDelete = this.stockAlertRepository.findById(createdDTO.getId());
        var deleted = this.alertService.deleteStockAlert("test@test.test", createdDTO.getId());
        Optional<StockAlert> afterDelete = this.stockAlertRepository.findById(createdDTO.getId());

        Assertions.assertTrue(deleted);
        Assertions.assertTrue(beforeDelete.isPresent());
        Assertions.assertTrue(afterDelete.isEmpty());
    }

    @Test
    public void deleteCryptoAlert() {
        CryptoAlertDTO cDTO = this.getCryptoAlertDTO();
        CryptoAlertDTO createdDTO = this.alertService.createNewCryptoAlert(this.user, cDTO);
        Optional<CryptoAlert> beforeDelete = this.cryptoAlertRepository.findById(createdDTO.getId());
        var deleted = this.alertService.deleteCryptoAlert("test@test.test", createdDTO.getId());
        Optional<CryptoAlert> afterDelete = this.cryptoAlertRepository.findById(createdDTO.getId());

        Assertions.assertTrue(deleted);
        Assertions.assertTrue(beforeDelete.isPresent());
        Assertions.assertTrue(afterDelete.isEmpty());
    }

    @Test
    public void deleteStockAlertNotFound() {
        Assertions.assertFalse(this.alertService.deleteStockAlert("test@test.test", 200L));
    }

    @Test
    public void deleteCryptoAlertNotFound() {
        Assertions.assertFalse(this.alertService.deleteCryptoAlert("test@test.test", 200L));
    }

    private StockAlertDTO getStockAlertDTO() {
        return StockAlertDTO.builder()
                .exchange("US")
                .type("PERCENT_OVER")
                .valuePoint(100.0)
                .shortName("SN")
                .name("Name")
                .build();
    }

    private CryptoAlertDTO getCryptoAlertDTO() {
        return CryptoAlertDTO.builder()
                .symbol("BTC")
                .type("PERCENT_OVER")
                .valuePoint(5.0)
                .name("Bitcoin")
                .build();
    }

    private StockAlertDTO convertToStockAlertDTO(StockAlert stockAlert) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(stockAlert, StockAlertDTO.class);
    }

    private CryptoAlertDTO convertToCryptoAlertDTO(CryptoAlert cryptoAlert) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(cryptoAlert, CryptoAlertDTO.class);
    }
}
