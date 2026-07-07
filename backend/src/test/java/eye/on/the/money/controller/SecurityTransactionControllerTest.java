package eye.on.the.money.controller;

import eye.on.the.money.dto.out.SecurityTransactionDTO;
import eye.on.the.money.service.security.SecurityTransactionService;
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
class SecurityTransactionControllerTest {

    @Mock
    private SecurityTransactionService securityTransactionService;

    @InjectMocks
    private SecurityTransactionController securityTransactionController;

    private List<SecurityTransactionDTO> createTransactionList() {
        List<SecurityTransactionDTO> list = new ArrayList<>();
        list.add(SecurityTransactionDTO.builder().transactionId(1L).buySell("B").quantity(10).amount(500.0)
                .transactionDate(LocalDate.now()).securityId("SEC1").securityName("Security One").currencyId("EUR").build());
        list.add(SecurityTransactionDTO.builder().transactionId(2L).buySell("S").quantity(5).amount(300.0)
                .transactionDate(LocalDate.now()).securityId("SEC2").securityName("Security Two").currencyId("USD").build());
        return list;
    }

    @Test
    void getAllTransactions() {
        List<SecurityTransactionDTO> transactions = this.createTransactionList();
        when(this.securityTransactionService.getTransactions("email")).thenReturn(transactions);

        Assertions.assertIterableEquals(transactions, this.securityTransactionController.getAllTransactions("email").getBody());
    }

    @Test
    void getHoldings() {
        List<SecurityTransactionDTO> holdings = this.createTransactionList();
        when(this.securityTransactionService.getCurrentHoldings("email")).thenReturn(holdings);

        Assertions.assertIterableEquals(holdings, this.securityTransactionController.getHoldings("email").getBody());
    }

    @Test
    void createTransaction() {
        SecurityTransactionDTO dto = SecurityTransactionDTO.builder().transactionId(1L).buySell("B").quantity(10)
                .amount(500.0).transactionDate(LocalDate.now()).securityId("SEC1").securityName("Security One").currencyId("EUR").build();
        when(this.securityTransactionService.createTransaction(dto, "email")).thenReturn(dto);

        Assertions.assertEquals(HttpStatus.CREATED, this.securityTransactionController.createTransaction("email", dto).getStatusCode());
        Assertions.assertEquals(dto, this.securityTransactionController.createTransaction("email", dto).getBody());
    }

    @Test
    void updateTransaction() {
        SecurityTransactionDTO dto = SecurityTransactionDTO.builder().transactionId(1L).buySell("B").quantity(15)
                .amount(750.0).transactionDate(LocalDate.now()).securityId("SEC1").securityName("Security One").currencyId("EUR").build();
        when(this.securityTransactionService.updateTransaction(dto, "email")).thenReturn(dto);

        Assertions.assertEquals(dto, this.securityTransactionController.updateTransaction("email", dto).getBody());
    }

    @Test
    void deleteByIds() {
        doNothing().when(this.securityTransactionService).deleteTransactionById(any(), any());

        Assertions.assertEquals(HttpStatus.OK, this.securityTransactionController.deleteByIds("email", List.of(1L, 2L)).getStatusCode());
    }

    @Test
    void getCSV() throws IOException {
        HttpServletResponse httpSR = new MockHttpServletResponse();
        doNothing().when(this.securityTransactionService).getCSV(any(), any());

        this.securityTransactionController.getCSV("email", httpSR);

        verify(this.securityTransactionService, times(1)).getCSV(any(), any());
    }

    @Test
    void processCSV() throws IOException {
        doNothing().when(this.securityTransactionService).processCSV(any(), any());

        Assertions.assertEquals(HttpStatus.CREATED, this.securityTransactionController.processCSV("email", null).getStatusCode());
    }
}
