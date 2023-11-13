package eye.on.the.money.service.impl;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.StockAlertDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.alert.StockAlert;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.repository.alert.StockAlertRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
public class AlertServiceImplTest {

    @Autowired
    private StockAlertRepository stockAlertRepository;

    @Autowired
    private AlertServiceImpl alertService;

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
    public void createNewStockAlert() {
        StockAlertDTO sDTO = this.getStockAlertDTO();
        StockAlertDTO createdDTO = this.alertService.createNewStockAlert(this.user, sDTO);
        StockAlert expected = this.stockAlertRepository.findById(createdDTO.getId()).get();

        Assertions.assertEquals(this.convertToStockAlertDTO(expected), createdDTO);
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

    private StockAlertDTO convertToStockAlertDTO(StockAlert stockAlert) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(stockAlert, StockAlertDTO.class);
    }
}
