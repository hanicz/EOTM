package eye.on.the.money.controller;

import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.crypto.TransactionService;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private final User user = User.builder().id(1L).email("email").build();

    @Test
    public void getCoinTransactionsByUserId() {
        List<TransactionDTO> tDTO = this.createTransactionList();

        when(this.transactionService.getTransactionsByUserId(1L)).thenReturn(tDTO);

        Assertions.assertIterableEquals(tDTO, this.transactionController.getCoinTransactionsByUserId(1L).getBody());
    }

    @Test
    public void getAllPositions() {
        List<TransactionDTO> tDTO = this.createTransactionList();

        when(this.transactionService.getAllPositions(1L)).thenReturn(tDTO);

        Assertions.assertIterableEquals(tDTO, this.transactionController.getAllPositions(1L).getBody());
    }

    @Test
    public void getAllHoldings() {
        TransactionQuery query = TransactionQuery.builder().currency("eur").build();
        List<TransactionDTO> tDTO = this.createTransactionList();

        when(this.transactionService.getCurrentHoldings(1L, query)).thenReturn(tDTO);

        Assertions.assertIterableEquals(tDTO, this.transactionController.getAllHoldings(1L, query, false).getBody());
    }

    @Test
    public void getAllHoldings_refreshBypassesTheCache() {
        TransactionQuery query = TransactionQuery.builder().currency("eur").build();
        List<TransactionDTO> tDTO = this.createTransactionList();

        when(this.transactionService.refreshCurrentHoldings(1L, query)).thenReturn(tDTO);

        Assertions.assertIterableEquals(tDTO, this.transactionController.getAllHoldings(1L, query, true).getBody());
        verify(this.transactionService, never()).getCurrentHoldings(1L, query);
    }

    @Test
    public void deleteByIds() {
        doNothing().when(this.transactionService).deleteTransactionById(any(), any());

        Assertions.assertEquals(HttpStatus.OK, this.transactionController.deleteByIds(1L, List.of(1L, 2L, 3L)).getStatusCode());
    }

    @Test
    public void getCSV() throws IOException {
        HttpServletResponse httpSR = new MockHttpServletResponse();

        doNothing().when(this.transactionService).getCSV(any(), any());
        this.transactionController.getCSV(1L, httpSR);

        verify(this.transactionService, times(1)).getCSV(any(), any());
    }

    @Test
    public void createTransaction() {
        TransactionDTO tDTO = TransactionDTO.builder().transactionString("s1").transactionDate(LocalDate.now()).amount(new BigDecimal("555.1")).quantity(new BigDecimal("431"))
                .buySell("b").symbol("s1").url("u1").fee(new BigDecimal("7")).currencyId("c1").coinId("co1").liveValue(new BigDecimal("33.1")).valueDiff(BigDecimal.ONE).build();

        when(this.transactionService.createTransaction(tDTO, 1L)).thenReturn(tDTO);

        Assertions.assertEquals(tDTO, this.transactionController.createTransaction(1L, tDTO).getBody());
    }

    @Test
    public void updateTransaction() {
        TransactionDTO tDTO = TransactionDTO.builder().id(1L).transactionString("s1").transactionDate(LocalDate.now()).amount(new BigDecimal("555.1")).quantity(new BigDecimal("431"))
                .buySell("b").symbol("s1").url("u1").fee(new BigDecimal("7")).currencyId("c1").coinId("co1").liveValue(new BigDecimal("33.1")).valueDiff(BigDecimal.ONE).build();

        when(this.transactionService.updateTransaction(tDTO, 1L)).thenReturn(tDTO);

        Assertions.assertEquals(tDTO, this.transactionController.updateTransaction(1L, tDTO).getBody());
    }

    @Test
    public void processCSV() throws IOException {
        MultipartFile mpf = new MockMultipartFile("mpf", "mpf.csv", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());

        doNothing().when(this.transactionService).processCSV(1L, mpf);

        Assertions.assertEquals(HttpStatus.CREATED, this.transactionController.processCSV(1L, mpf).getStatusCode());
    }

    private List<TransactionDTO> createTransactionList() {
        List<TransactionDTO> tDTO = new ArrayList<>();
        tDTO.add(TransactionDTO.builder().id(1L).transactionString("s1").transactionDate(LocalDate.now()).amount(new BigDecimal("555.1")).quantity(new BigDecimal("431"))
                .buySell("b").symbol("s1").url("u1").fee(new BigDecimal("7")).currencyId("c1").coinId("co1").liveValue(new BigDecimal("33.1")).valueDiff(BigDecimal.ONE).build());
        tDTO.add(TransactionDTO.builder().id(2L).transactionString("s2").transactionDate(LocalDate.now()).amount(new BigDecimal("1555.1")).quantity(new BigDecimal("3431"))
                .buySell("s").symbol("s2").url("u2").fee(new BigDecimal("7.2")).currencyId("c2").coinId("co2").liveValue(new BigDecimal("233.1")).valueDiff(new BigDecimal("1.1")).build());
        tDTO.add(TransactionDTO.builder().id(3L).transactionString("s3").transactionDate(LocalDate.now()).amount(new BigDecimal("5553.1")).quantity(new BigDecimal("4321"))
                .buySell("b").symbol("s3").url("u3").fee(new BigDecimal("7.3")).currencyId("c3").coinId("co3").liveValue(new BigDecimal("313.1")).valueDiff(new BigDecimal("22")).build());

        return tDTO;
    }
}