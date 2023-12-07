package eye.on.the.money.controller;

import eye.on.the.money.dto.in.InvestmentQuery;
import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.etf.ETFInvestmentService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class ETFControllerTest {

    @Mock
    private ETFInvestmentService etfInvestmentService;

    @InjectMocks
    private ETFController etfController;

    private final User user = User.builder().id(1L).build();

    @Test
    public void getAllETFInvestments() {
        List<ETFInvestmentDTO> eiDTO = this.createETFList();

        when(this.etfInvestmentService.getETFInvestments(this.user.getId())).thenReturn(eiDTO);

        Assertions.assertIterableEquals(eiDTO, this.etfController.getAllETFInvestments(this.user).getBody());
    }

    @Test
    public void getETFHoldings() {
        InvestmentQuery query = InvestmentQuery.builder().currency("eur").build();
        List<ETFInvestmentDTO> eiDTO = this.createETFList();

        when(this.etfInvestmentService.getCurrentETFHoldings(this.user.getId(), query)).thenReturn(eiDTO);

        Assertions.assertIterableEquals(eiDTO, this.etfController.getETFHoldings(this.user, query).getBody());
    }

    @Test
    public void getPositions() {
        InvestmentQuery query = InvestmentQuery.builder().currency("eur").build();
        List<ETFInvestmentDTO> eiDTO = this.createETFList();

        when(this.etfInvestmentService.getAllPositions(this.user.getId(), query)).thenReturn(eiDTO);

        Assertions.assertIterableEquals(eiDTO, this.etfController.getPositions(this.user, query).getBody());
    }

    @Test
    public void createInvestment() {
        ETFInvestmentDTO eiDTO = ETFInvestmentDTO.builder().valueDiff(0.1).transactionDate(new Date()).id(1L).fee(7.0).liveValue(55.6)
                .shortName("s1").buySell("b").exchange("e1").currencyId("eur").quantity(645).build();

        when(this.etfInvestmentService.createInvestment(eiDTO, user)).thenReturn(eiDTO);

        Assertions.assertEquals(eiDTO, this.etfController.createInvestment(user, eiDTO).getBody());

    }

    @Test
    public void deleteByIds() {
        doNothing().when(this.etfInvestmentService).deleteInvestmentById(any(), any());

        Assertions.assertEquals(HttpStatus.OK, this.etfController.deleteByIds(user, "1,2,3").getStatusCode());
    }

    @Test
    public void getCSV() throws IOException {
        HttpServletResponse httpSR = new MockHttpServletResponse();

        doNothing().when(this.etfInvestmentService).getCSV(any(), any());
        this.etfController.getCSV(user, httpSR);

        verify(this.etfInvestmentService, times(1)).getCSV(any(), any());
    }

    @Test
    public void updateInvestment() {
        ETFInvestmentDTO eiDTO = ETFInvestmentDTO.builder().valueDiff(0.1).transactionDate(new Date()).id(1L).fee(7.0).liveValue(55.6)
                .shortName("s1").buySell("b").exchange("e1").currencyId("eur").quantity(645).build();

        when(this.etfInvestmentService.updateInvestment(eiDTO, user)).thenReturn(eiDTO);

        Assertions.assertEquals(eiDTO, this.etfController.updateInvestment(user, eiDTO).getBody());
    }

    private List<ETFInvestmentDTO> createETFList() {
        List<ETFInvestmentDTO> eiDTO = new ArrayList<>();
        eiDTO.add(ETFInvestmentDTO.builder().valueDiff(0.1).transactionDate(new Date()).id(1L).fee(7.0).liveValue(55.6)
                .shortName("s1").buySell("b").exchange("e1").currencyId("eur").quantity(645).build());
        eiDTO.add(ETFInvestmentDTO.builder().valueDiff(0.2).transactionDate(new Date()).id(2L).fee(7.3).liveValue(455.6)
                .shortName("s2").buySell("b").exchange("e2").currencyId("usd").quantity(5423).build());
        eiDTO.add(ETFInvestmentDTO.builder().valueDiff(0.5).transactionDate(new Date()).id(3L).fee(7.02).liveValue(551.6)
                .shortName("s3").buySell("s").exchange("e3").currencyId("huf").quantity(4231).build());

        return eiDTO;
    }
}