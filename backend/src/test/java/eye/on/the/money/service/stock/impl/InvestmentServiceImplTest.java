package eye.on.the.money.service.stock.impl;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Investment;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.stock.InvestmentRepository;
import eye.on.the.money.repository.stock.StockRepository;
import eye.on.the.money.service.api.EODAPIService;
import eye.on.the.money.service.stock.StockPaymentService;
import eye.on.the.money.service.stock.StockService;
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

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class InvestmentServiceImplTest {

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
    private InvestmentServiceImpl investmentService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    public void init() {
        this.user = this.userRepository.findByEmail("test@test.test");
    }

    @Test
    public void getInvestments() {
        List<InvestmentDTO> result = this.investmentService.getInvestments(this.user.getId());
        List<Investment> investments = this.investmentRepository.findByUser_IdOrderByTransactionDate(this.user.getId());

        Assertions.assertIterableEquals(investments.stream().map(this::convertToInvestmentDTO).collect(Collectors.toList()), result);
    }

    private InvestmentDTO convertToInvestmentDTO(Investment investment) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(investment, InvestmentDTO.class);
    }
}