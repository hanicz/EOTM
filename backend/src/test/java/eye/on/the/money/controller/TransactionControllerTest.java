package eye.on.the.money.controller;

import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.crypto.TransactionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private final User user = User.builder().id(1L).build();

    @Test
    public void getCoinTransactionsByUserId() {
        List<TransactionDTO> tDTO = this.createTransactionList();

        when(this.transactionService.getTransactionsByUserId(this.user.getId())).thenReturn(tDTO);

        Assertions.assertIterableEquals(tDTO, this.transactionController.getCoinTransactionsByUserId(this.user).getBody());
    }

    @Test
    public void getAllPositions() {
        List<TransactionDTO> tDTO = this.createTransactionList();

        when(this.transactionService.getAllPositions(this.user.getId())).thenReturn(tDTO);

        Assertions.assertIterableEquals(tDTO, this.transactionController.getAllPositions(this.user).getBody());
    }

    @Test
    public void getAllHoldings() {
        TransactionQuery query = TransactionQuery.builder().currency("eur").build();
        List<TransactionDTO> tDTO = this.createTransactionList();

        when(this.transactionService.getCurrentHoldings(this.user.getId(), query)).thenReturn(tDTO);

        Assertions.assertIterableEquals(tDTO, this.transactionController.getAllHoldings(this.user, query).getBody());
    }

    @Test
    public void deleteByIds() {
        doNothing().when(this.transactionService).deleteTransactionById(any(), any());

        Assertions.assertEquals(HttpStatus.OK, this.transactionController.deleteByIds(this.user, "1,2,3").getStatusCode());
    }

    @Test
    public void getCSV() throws IOException {
        HttpServletResponse httpSR = new MockHttpServletResponse();

        doNothing().when(this.transactionService).getCSV(any(), any());
        this.transactionController.getCSV(this.user, httpSR);

        verify(this.transactionService, times(1)).getCSV(any(), any());
    }

    @Test
    public void createTransaction() {
        TransactionDTO tDTO = TransactionDTO.builder().transactionString("s1").transactionDate(new Date()).amount(555.1).quantity(431.0)
                .buySell("b").symbol("s1").url("u1").fee(7.0).currencyId("c1").coinId("co1").liveValue(33.1).valueDiff(1.0).build();

        when(this.transactionService.createTransaction(tDTO, this.user)).thenReturn(tDTO);

        Assertions.assertEquals(tDTO, this.transactionController.createTransaction(this.user, tDTO).getBody());
    }

    @Test
    public void updateTransaction() {
        TransactionDTO tDTO = TransactionDTO.builder().transactionId(1L).transactionString("s1").transactionDate(new Date()).amount(555.1).quantity(431.0)
                .buySell("b").symbol("s1").url("u1").fee(7.0).currencyId("c1").coinId("co1").liveValue(33.1).valueDiff(1.0).build();

        when(this.transactionService.updateTransaction(tDTO, this.user)).thenReturn(tDTO);

        Assertions.assertEquals(tDTO, this.transactionController.updateTransaction(this.user, tDTO).getBody());
    }

    @Test
    public void processCSV() throws IOException {
        MultipartFile mpf = new MockMultipartFile("mpf", "mpf.csv", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());

        doNothing().when(this.transactionService).processCSV(this.user, mpf);

        Assertions.assertEquals(HttpStatus.CREATED, this.transactionController.processCSV(this.user, mpf).getStatusCode());
    }

    private List<TransactionDTO> createTransactionList() {
        List<TransactionDTO> tDTO = new ArrayList<>();
        tDTO.add(TransactionDTO.builder().transactionId(1L).transactionString("s1").transactionDate(new Date()).amount(555.1).quantity(431.0)
                .buySell("b").symbol("s1").url("u1").fee(7.0).currencyId("c1").coinId("co1").liveValue(33.1).valueDiff(1.0).build());
        tDTO.add(TransactionDTO.builder().transactionId(2L).transactionString("s2").transactionDate(new Date()).amount(1555.1).quantity(3431.0)
                .buySell("s").symbol("s2").url("u2").fee(7.2).currencyId("c2").coinId("co2").liveValue(233.1).valueDiff(1.1).build());
        tDTO.add(TransactionDTO.builder().transactionId(3L).transactionString("s3").transactionDate(new Date()).amount(5553.1).quantity(4321.0)
                .buySell("b").symbol("s3").url("u3").fee(7.3).currencyId("c3").coinId("co3").liveValue(313.1).valueDiff(22.0).build());

        return tDTO;
    }
}