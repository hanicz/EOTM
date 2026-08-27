package eye.on.the.money.controller;

import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.stock.InvestmentService;
import eye.on.the.money.service.stock.RSUTaxService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class InvestmentControllerTest {

    @Mock
    private InvestmentService investmentService;

    @Mock
    private RSUTaxService rsuTaxService;

    @InjectMocks
    private InvestmentController investmentController;

    private final User user = User.builder().id(1L).email("email").build();

    @Test
    public void getAllInvestments() {
        List<InvestmentDTO> iDTO = this.createInvestmentList();

        when(this.investmentService.getInvestments(1L)).thenReturn(iDTO);

        Assertions.assertIterableEquals(iDTO, this.investmentController.getAllInvestments(1L).getBody());
    }

    @Test
    public void getHoldings() {
        List<InvestmentDTO> iDTO = this.createInvestmentList();

        when(this.investmentService.getCurrentHoldings(1L)).thenReturn(iDTO);

        Assertions.assertIterableEquals(iDTO, this.investmentController.getHoldings(1L, false).getBody());
    }

    @Test
    public void getHoldings_refreshBypassesTheCache() {
        List<InvestmentDTO> iDTO = this.createInvestmentList();

        when(this.investmentService.refreshCurrentHoldings(1L)).thenReturn(iDTO);

        Assertions.assertIterableEquals(iDTO, this.investmentController.getHoldings(1L, true).getBody());
        verify(this.investmentService, never()).getCurrentHoldings(1L);
    }

    @Test
    public void getPositions() {
        List<InvestmentDTO> iDTO = this.createInvestmentList();

        when(this.investmentService.getAllPositions(1L)).thenReturn(iDTO);

        Assertions.assertIterableEquals(iDTO, this.investmentController.getPositions(1L).getBody());
    }

    @Test
    public void createInvestment() {
        InvestmentDTO iDTO = InvestmentDTO.builder().buySell("b").amount(3213.0).fee(7.8).quantity(33).name("n1")
                .exchange("e1").shortName("s1").transactionDate(LocalDate.now()).currencyId("eur").liveValue(674.1).valueDiff(4.0).build();

        when(this.investmentService.createInvestment(iDTO, 1L)).thenReturn(iDTO);

        Assertions.assertEquals(iDTO, this.investmentController.createInvestment(1L, iDTO).getBody());
    }

    @Test
    public void deleteByIds() {
        doNothing().when(this.investmentService).deleteInvestmentById(any(), any());

        Assertions.assertEquals(HttpStatus.OK, this.investmentController.deleteByIds(1L, List.of(1L, 2L, 3L)).getStatusCode());
    }

    @Test
    public void setRSU() {
        doNothing().when(this.rsuTaxService).setRSU(any(), any(), anyBoolean());

        Assertions.assertEquals(HttpStatus.OK,
                this.investmentController.setRSU(1L, List.of(1L, 2L), true).getStatusCode());
        verify(this.rsuTaxService).setRSU(1L, List.of(1L, 2L), true);
    }

    @Test
    public void getCSV() throws IOException {
        HttpServletResponse httpSR = new MockHttpServletResponse();

        doNothing().when(this.investmentService).getCSV(any(), any());
        this.investmentController.getCSV(1L, httpSR);

        verify(this.investmentService, times(1)).getCSV(any(), any());
    }

    @Test
    public void updateInvestment() {
        InvestmentDTO iDTO = InvestmentDTO.builder().investmentId(1L).buySell("b").amount(3213.0).fee(7.8).quantity(33).name("n1")
                .exchange("e1").shortName("s1").transactionDate(LocalDate.now()).currencyId("eur").liveValue(674.1).valueDiff(4.0).build();

        when(this.investmentService.updateInvestment(iDTO, 1L)).thenReturn(iDTO);

        Assertions.assertEquals(iDTO, this.investmentController.updateInvestment(1L, iDTO).getBody());
    }

    @Test
    public void processCSV() throws IOException {
        MultipartFile mpf = new MockMultipartFile("mpf", "mpf.csv", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());

        doNothing().when(this.investmentService).processCSV(1L, mpf);

        Assertions.assertEquals(HttpStatus.CREATED, this.investmentController.processCSV(1L, mpf).getStatusCode());
    }

    private List<InvestmentDTO> createInvestmentList() {
        List<InvestmentDTO> iDTO = new ArrayList<>();
        iDTO.add(InvestmentDTO.builder().investmentId(1L).buySell("b").amount(3213.0).fee(7.8).quantity(33).name("n1")
                .exchange("e1").shortName("s1").transactionDate(LocalDate.now()).currencyId("eur").liveValue(674.1).valueDiff(4.0).build());
        iDTO.add(InvestmentDTO.builder().investmentId(2L).buySell("s").amount(32133.0).fee(7.1).quantity(40).name("n2")
                .exchange("e2").shortName("s2").transactionDate(LocalDate.now()).currencyId("eur").liveValue(267.1).valueDiff(4.2).build());
        iDTO.add(InvestmentDTO.builder().investmentId(3L).buySell("b").amount(3213.2).fee(73.8).quantity(38).name("n3")
                .exchange("e3").shortName("s3").transactionDate(LocalDate.now()).currencyId("eur").liveValue(637.1).valueDiff(4.3).build());

        return iDTO;
    }
}