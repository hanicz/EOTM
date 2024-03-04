package eye.on.the.money.service.stock;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Investment;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.stock.InvestmentRepository;
import eye.on.the.money.repository.stock.StockRepository;
import eye.on.the.money.service.api.EODAPIService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class InvestmentServiceTest {

    @Autowired
    private InvestmentRepository investmentRepository;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private CurrencyRepository currencyRepository;
    @MockBean
    private StockPaymentService stockPaymentService;
    @MockBean
    private EODAPIService eodAPIService;
    @MockBean
    private StockService stockService;
    @Autowired
    private InvestmentService investmentService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private UserRepository userRepository;
    private User user;
    double epsilon = 0.000001d;

    @BeforeEach
    public void init() {
        this.user = this.userRepository.findByEmail("test@test.test");
    }

    @Test
    public void getInvestments() {
        List<InvestmentDTO> result = this.investmentService.getInvestments(this.user.getUsername());
        List<Investment> investments = this.investmentRepository.findByUserEmailOrderByTransactionDateDesc(this.user.getUsername());

        Assertions.assertIterableEquals(investments.stream().map(this::convertToInvestmentDTO).collect(Collectors.toList()), result);
    }

    @Test
    public void getAllPositions() {
        List<InvestmentDTO> result = this.investmentService.getAllPositions(this.user.getUsername());
        InvestmentDTO testObject = result.stream().filter(iDTO -> "CRSR".equals(iDTO.getShortName())).findAny().get();

        Assertions.assertAll("Assert all merged values",
                () -> assertEquals("B", testObject.getBuySell()),
                () -> assertEquals(0, testObject.getQuantity()),
                () -> assertEquals(-100.0, testObject.getAmount(), this.epsilon));
    }

    @Test
    public void getAllPositions2() {
        List<InvestmentDTO> result = this.investmentService.getAllPositions(this.user.getUsername());
        InvestmentDTO testObject = result.stream().filter(iDTO -> "AMD".equals(iDTO.getShortName())).findAny().get();

        Assertions.assertAll("Assert all merged values",
                () -> assertEquals("B", testObject.getBuySell()),
                () -> assertEquals(36, testObject.getQuantity()),
                () -> assertEquals(-189.9, testObject.getAmount(), this.epsilon));
    }

    @Test
    public void getAllPositions3() {
        List<InvestmentDTO> result = this.investmentService.getAllPositions(this.user.getUsername());
        InvestmentDTO testObject = result.stream().filter(iDTO -> "INTC".equals(iDTO.getShortName())).findAny().get();

        Assertions.assertAll("Assert all merged values",
                () -> assertEquals("B", testObject.getBuySell()),
                () -> assertEquals(2, testObject.getQuantity()),
                () -> assertEquals(43.77, testObject.getAmount(), this.epsilon));
    }

    @Test
    public void getCurrentHoldings() {
        List<InvestmentDTO> result = this.investmentService.getAllPositions(this.user.getUsername());
        InvestmentDTO testObject = result.stream().filter(iDTO -> "CRSR".equals(iDTO.getShortName())).findAny().get();

        Assertions.assertAll("Assert all merged values",
                () -> assertEquals("B", testObject.getBuySell()),
                () -> assertEquals(0, testObject.getQuantity()),
                () -> assertEquals(-100.0, testObject.getAmount(), this.epsilon));
    }

    private InvestmentDTO convertToInvestmentDTO(Investment investment) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(investment, InvestmentDTO.class);
    }
}