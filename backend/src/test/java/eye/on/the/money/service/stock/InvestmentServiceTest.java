package eye.on.the.money.service.stock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.financial.TaxDetails;
import eye.on.the.money.model.stock.Investment;
import eye.on.the.money.model.stock.RSUTaxDetails;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.repository.stock.InvestmentRepository;
import eye.on.the.money.repository.stock.RSUTaxDetailsRepository;
import eye.on.the.money.service.api.EODAPIService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class InvestmentServiceTest {

    @Autowired
    private InvestmentRepository investmentRepository;
    @Autowired
    private RSUTaxDetailsRepository rsuTaxDetailsRepository;
    @MockitoBean
    private EODAPIService eodAPIService;
    @MockitoBean
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
        List<InvestmentDTO> result = this.investmentService.getInvestments(this.user.getId());
        List<Investment> investments = this.investmentRepository.findByUserIdOrderByTransactionDateDesc(this.user.getId());

        Assertions.assertIterableEquals(investments.stream().map(this::convertToInvestmentDTO).collect(Collectors.toList()), result);
    }

    @Test
    public void getAllPositions() {
        List<InvestmentDTO> result = this.investmentService.getAllPositions(this.user.getId());
        InvestmentDTO testObject = result.stream().filter(iDTO -> "CRSR".equals(iDTO.getShortName())).findAny().get();

        Assertions.assertAll("Assert all merged values",
                () -> assertEquals("B", testObject.getBuySell()),
                () -> assertEquals(0, testObject.getQuantity()),
                () -> assertEquals(-100.0, testObject.getAmount(), this.epsilon));
    }

    @Test
    public void getAllPositions2() {
        List<InvestmentDTO> result = this.investmentService.getAllPositions(this.user.getId());
        InvestmentDTO testObject = result.stream().filter(iDTO -> "AMD".equals(iDTO.getShortName())).findAny().get();

        Assertions.assertAll("Assert all merged values",
                () -> assertEquals("B", testObject.getBuySell()),
                () -> assertEquals(36, testObject.getQuantity()),
                () -> assertEquals(-189.9, testObject.getAmount(), this.epsilon));
    }

    @Test
    public void getAllPositions3() {
        List<InvestmentDTO> result = this.investmentService.getAllPositions(this.user.getId());
        InvestmentDTO testObject = result.stream().filter(iDTO -> "INTC".equals(iDTO.getShortName())).findAny().get();

        Assertions.assertAll("Assert all merged values",
                () -> assertEquals("B", testObject.getBuySell()),
                () -> assertEquals(2, testObject.getQuantity()),
                () -> assertEquals(43.77, testObject.getAmount(), this.epsilon));
    }

    @Test
    public void getCurrentHoldings() {
        List<InvestmentDTO> result = this.investmentService.getAllPositions(this.user.getId());
        InvestmentDTO testObject = result.stream().filter(iDTO -> "CRSR".equals(iDTO.getShortName())).findAny().get();

        Assertions.assertAll("Assert all merged values",
                () -> assertEquals("B", testObject.getBuySell()),
                () -> assertEquals(0, testObject.getQuantity()),
                () -> assertEquals(-100.0, testObject.getAmount(), this.epsilon));
    }

    @Test
    public void getCurrentHoldingsWithoutInvestments() {
        List<InvestmentDTO> result = this.investmentService.getCurrentHoldings(-1L);

        Assertions.assertTrue(result.isEmpty());
        verifyNoInteractions(this.eodAPIService);
    }

    @Test
    public void refreshCurrentHoldingsMatchesGetCurrentHoldings() throws JsonProcessingException {
        when(this.eodAPIService.getLiveStockValue(anyString())).thenReturn(new ObjectMapper().readTree("[]"));

        List<InvestmentDTO> cached = this.investmentService.getCurrentHoldings(this.user.getId());
        List<InvestmentDTO> refreshed = this.investmentService.refreshCurrentHoldings(this.user.getId());

        Assertions.assertIterableEquals(cached, refreshed);
    }

    @Test
    public void currentHoldingsCarryTheDailyChange() throws JsonProcessingException {
        when(this.eodAPIService.getLiveStockValue(anyString())).thenReturn(new ObjectMapper().readTree("""
                [{"code":"GOOG.US","close":30.0,"previousClose":27.5,"change":2.5,"change_p":9.0909},
                 {"code":"INTC.US","close":"NA","previousClose":21.0,"change":"NA","change_p":"NA"}]"""));

        List<InvestmentDTO> result = this.investmentService.getCurrentHoldings(this.user.getId());
        InvestmentDTO goog = result.stream().filter(iDTO -> "GOOG".equals(iDTO.getShortName())).findAny().orElseThrow();
        InvestmentDTO intc = result.stream().filter(iDTO -> "INTC".equals(iDTO.getShortName())).findAny().orElseThrow();

        Assertions.assertAll("The daily change scales with quantity and stays absent for an unquoted holding",
                () -> assertEquals(12.5, goog.getDayChange(), this.epsilon),
                () -> assertEquals(9.0909, goog.getDayChangePercent(), this.epsilon),
                () -> assertEquals(42.0, intc.getLiveValue(), this.epsilon),
                () -> Assertions.assertTrue(intc.getStalePrice()),
                () -> Assertions.assertNull(intc.getDayChange()),
                () -> Assertions.assertNull(intc.getDayChangePercent()));
    }

    @Test
    public void getAllPositionsReopenedLotIsNotMergedWithClosedLot() {
        List<InvestmentDTO> result = this.investmentService.getAllPositions(this.user.getId());
        List<InvestmentDTO> googPositions = result.stream().filter(iDTO -> "GOOG".equals(iDTO.getShortName())).toList();

        InvestmentDTO closedLot = googPositions.stream().filter(iDTO -> iDTO.getQuantity() == 0).findAny().get();
        InvestmentDTO openLot = googPositions.stream().filter(iDTO -> iDTO.getQuantity() > 0).findAny().get();

        Assertions.assertAll("Closed and reopened lots must be tracked separately",
                () -> assertEquals(2, googPositions.size()),
                () -> assertEquals(-50.0, closedLot.getAmount(), this.epsilon),
                () -> assertEquals(5, openLot.getQuantity()),
                () -> assertEquals(50.0, openLot.getAmount(), this.epsilon));
    }

    @Test
    public void getPositionsByAccountIdOnlyOpenLotHasPositiveQuantity() {
        List<InvestmentDTO> result = this.investmentService.getPositionsByAccountId(this.user.getId(), 1L);
        List<InvestmentDTO> googHoldings = result.stream()
                .filter(iDTO -> "GOOG".equals(iDTO.getShortName()) && iDTO.getQuantity() > 0).toList();

        Assertions.assertAll("Only the reopened lot should count towards current holdings",
                () -> assertEquals(1, googHoldings.size()),
                () -> assertEquals(5, googHoldings.get(0).getQuantity()),
                () -> assertEquals(50.0, googHoldings.get(0).getAmount(), this.epsilon));
    }

    @Test
    @Transactional
    public void updateInvestmentClearsTheRSUFreezeWhenTheValuationInputsChange() {
        Investment investment = this.flagAsRSU();
        InvestmentDTO investmentDTO = this.convertToInvestmentDTO(investment);
        investmentDTO.setTransactionDate(investmentDTO.getTransactionDate().plusDays(1));
        when(this.stockService.getOrCreateStock(anyString(), anyString(), anyString()))
                .thenReturn(investment.getStock());

        this.investmentService.updateInvestment(investmentDTO, this.user.getId());

        Assertions.assertFalse(investment.isRsu());
        Assertions.assertTrue(this.rsuTaxDetailsRepository
                .findByUserIdAndInvestmentIdIn(this.user.getId(), List.of(investment.getId())).isEmpty());
    }

    @Test
    @Transactional
    public void updateInvestmentKeepsTheRSUFreezeWhenTheValuationInputsAreUnchanged() {
        Investment investment = this.flagAsRSU();
        InvestmentDTO investmentDTO = this.convertToInvestmentDTO(investment);
        investmentDTO.setFee(9.99);
        when(this.stockService.getOrCreateStock(anyString(), anyString(), anyString()))
                .thenReturn(investment.getStock());

        this.investmentService.updateInvestment(investmentDTO, this.user.getId());

        Assertions.assertTrue(investment.isRsu());
        Assertions.assertEquals(1, this.rsuTaxDetailsRepository
                .findByUserIdAndInvestmentIdIn(this.user.getId(), List.of(investment.getId())).size());
    }

    private Investment flagAsRSU() {
        Investment investment = this.investmentRepository
                .findByUserIdOrderByTransactionDateDesc(this.user.getId()).stream()
                .filter(i -> "B".equals(i.getBuySell())).findFirst().orElseThrow();
        investment.setRsu(true);
        this.investmentRepository.save(investment);
        this.rsuTaxDetailsRepository.save(RSUTaxDetails.builder()
                .investment(investment)
                .price(new BigDecimal("214.50"))
                .priceDate(investment.getTransactionDate())
                .currency("USD")
                .taxDetails(TaxDetails.builder()
                        .rate(new BigDecimal("368.42"))
                        .rateDate(investment.getTransactionDate())
                        .amountInHuf(new BigDecimal("3951304.50"))
                        .taxBase(new BigDecimal("3516661.01"))
                        .szocho(new BigDecimal("457166"))
                        .szja(new BigDecimal("527499"))
                        .total(new BigDecimal("984665"))
                        .calculatedOn(LocalDate.now())
                        .build())
                .build());
        return investment;
    }

    private InvestmentDTO convertToInvestmentDTO(Investment investment) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(investment, InvestmentDTO.class);
    }
}