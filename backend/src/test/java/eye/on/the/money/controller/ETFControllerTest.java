package eye.on.the.money.controller;

import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.etf.ETFInvestmentService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ETFControllerTest {

    @Mock
    private ETFInvestmentService etfInvestmentService;

    @InjectMocks
    private ETFController etfController;

    private final User user = User.builder().id(1L).email("email").build();

    @Test
    public void getAllETFInvestments() {
        List<ETFInvestmentDTO> eiDTO = this.createETFList();

        when(this.etfInvestmentService.getETFInvestments("email")).thenReturn(eiDTO);

        Assertions.assertIterableEquals(eiDTO, this.etfController.getAllETFInvestments("email").getBody());
    }

    @Test
    public void getETFHoldings() {
        List<ETFInvestmentDTO> eiDTO = this.createETFList();

        when(this.etfInvestmentService.getCurrentETFHoldings("email")).thenReturn(eiDTO);

        Assertions.assertIterableEquals(eiDTO, this.etfController.getETFHoldings("email").getBody());
    }

    @Test
    public void getPositions() {
        List<ETFInvestmentDTO> eiDTO = this.createETFList();

        when(this.etfInvestmentService.getAllPositions("email")).thenReturn(eiDTO);

        Assertions.assertIterableEquals(eiDTO, this.etfController.getPositions("email").getBody());
    }

    @Test
    public void createInvestment() {
        ETFInvestmentDTO eiDTO = ETFInvestmentDTO.builder().valueDiff(0.1).transactionDate(LocalDate.now()).id(1L).fee(7.0).liveValue(55.6)
                .shortName("s1").buySell("b").exchange("e1").currencyId("eur").quantity(645).build();

        when(this.etfInvestmentService.createInvestment(eiDTO, "email")).thenReturn(eiDTO);

        Assertions.assertEquals(eiDTO, this.etfController.createInvestment("email", eiDTO).getBody());

    }

    @Test
    public void deleteByIds() {
        doNothing().when(this.etfInvestmentService).deleteInvestmentById(any(), any());

        Assertions.assertEquals(HttpStatus.OK, this.etfController.deleteByIds("email", List.of(1L, 2L, 3L)).getStatusCode());
    }

    @Test
    public void getCSV() throws IOException {
        HttpServletResponse httpSR = new MockHttpServletResponse();

        doNothing().when(this.etfInvestmentService).getCSV(any(), any());
        this.etfController.getCSV("email", httpSR);

        verify(this.etfInvestmentService, times(1)).getCSV(any(), any());
    }

    @Test
    public void updateInvestment() {
        ETFInvestmentDTO eiDTO = ETFInvestmentDTO.builder().valueDiff(0.1).transactionDate(LocalDate.now()).id(1L).fee(7.0).liveValue(55.6)
                .shortName("s1").buySell("b").exchange("e1").currencyId("eur").quantity(645).build();

        when(this.etfInvestmentService.updateInvestment(eiDTO, "email")).thenReturn(eiDTO);

        Assertions.assertEquals(eiDTO, this.etfController.updateInvestment("email", eiDTO).getBody());
    }

    private List<ETFInvestmentDTO> createETFList() {
        List<ETFInvestmentDTO> eiDTO = new ArrayList<>();
        eiDTO.add(ETFInvestmentDTO.builder().valueDiff(0.1).transactionDate(LocalDate.now()).id(1L).fee(7.0).liveValue(55.6)
                .shortName("s1").buySell("b").exchange("e1").currencyId("eur").quantity(645).build());
        eiDTO.add(ETFInvestmentDTO.builder().valueDiff(0.2).transactionDate(LocalDate.now()).id(2L).fee(7.3).liveValue(455.6)
                .shortName("s2").buySell("b").exchange("e2").currencyId("usd").quantity(5423).build());
        eiDTO.add(ETFInvestmentDTO.builder().valueDiff(0.5).transactionDate(LocalDate.now()).id(3L).fee(7.02).liveValue(551.6)
                .shortName("s3").buySell("s").exchange("e3").currencyId("huf").quantity(4231).build());

        return eiDTO;
    }
}