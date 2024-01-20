package eye.on.the.money.controller;

import eye.on.the.money.dto.out.ForexTransactionDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.forex.ForexTransactionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class ForexControllerTest {

    @Mock
    private ForexTransactionService forexTransactionService;

    @InjectMocks
    private ForexController forexController;

    private final User user = User.builder().id(1L).email("email").build();

    @Test
    public void getForexTransactionsByUserId() {
        List<ForexTransactionDTO> ftDTO = new ArrayList<>();

        when(this.forexTransactionService.getForexTransactionsByUserId("email")).thenReturn(ftDTO);

        Assertions.assertIterableEquals(ftDTO, this.forexController.getForexTransactionsByUserId(this.user).getBody());
    }

    @Test
    public void getForexHoldings() {
        List<ForexTransactionDTO> ftDTO = new ArrayList<>();

        when(this.forexTransactionService.getAllForexHoldings("email")).thenReturn(ftDTO);

        Assertions.assertIterableEquals(ftDTO, this.forexController.getForexHoldings(this.user).getBody());
    }

    @Test
    public void deleteByIds() {
        doNothing().when(this.forexTransactionService).deleteForexTransactionById(any(), any());

        Assertions.assertEquals(HttpStatus.OK, this.forexController.deleteByIds(user, "1,2,3").getStatusCode());
    }

    @Test
    public void createTransaction() {
        ForexTransactionDTO ftDTO = ForexTransactionDTO.builder().toAmount(1.0).fromAmount(3.0).transactionDate(new Date())
                .buySell("b").changeRate(55.6).liveChangeRate(66.7).liveValue(100.1).valueDiff(5.6).fromCurrencyId("eur").toCurrencyId("usd").build();

        when(this.forexTransactionService.createForexTransaction(ftDTO, "email")).thenReturn(ftDTO);

        Assertions.assertEquals(ftDTO, this.forexController.createTransaction(this.user, ftDTO).getBody());
    }

    @Test
    public void updateTransaction() {
        ForexTransactionDTO ftDTO = ForexTransactionDTO.builder().toAmount(1.0).fromAmount(3.0).transactionDate(new Date())
                .buySell("b").changeRate(55.6).liveChangeRate(66.7).liveValue(100.1).valueDiff(5.6).fromCurrencyId("eur").toCurrencyId("usd").build();

        when(this.forexTransactionService.updateForexTransaction(ftDTO, "email")).thenReturn(ftDTO);

        Assertions.assertEquals(ftDTO, this.forexController.updateTransaction(this.user, ftDTO).getBody());
    }

    private List<ForexTransactionDTO> createTransactionList() {
        List<ForexTransactionDTO> ftDTO = new ArrayList<>();
        ftDTO.add(ForexTransactionDTO.builder().toAmount(1.0).fromAmount(3.0).forexTransactionId(1L).transactionDate(new Date())
                .buySell("b").changeRate(55.6).liveChangeRate(66.7).liveValue(100.1).valueDiff(5.6).fromCurrencyId("eur").toCurrencyId("usd").build());
        ftDTO.add(ForexTransactionDTO.builder().toAmount(1.0).fromAmount(3.0).forexTransactionId(1L).transactionDate(new Date())
                .buySell("b").changeRate(55.6).liveChangeRate(66.7).liveValue(100.1).valueDiff(5.6).fromCurrencyId("eur").toCurrencyId("usd").build());
        ftDTO.add(ForexTransactionDTO.builder().toAmount(1.0).fromAmount(3.0).forexTransactionId(1L).transactionDate(new Date())
                .buySell("b").changeRate(55.6).liveChangeRate(66.7).liveValue(100.1).valueDiff(5.6).fromCurrencyId("eur").toCurrencyId("usd").build());

        return ftDTO;
    }
}