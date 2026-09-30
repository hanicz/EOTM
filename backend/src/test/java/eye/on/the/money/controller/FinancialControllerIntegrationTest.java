package eye.on.the.money.controller;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.in.BankTransactionFlag;
import eye.on.the.money.dto.in.BankTransactionQuery;
import eye.on.the.money.dto.out.BankTransactionDTO;
import eye.on.the.money.dto.out.CategoryMatchDTO;
import eye.on.the.money.dto.out.PageDTO;
import eye.on.the.money.service.financial.BankCategoryRuleService;
import eye.on.the.money.service.financial.BankTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class FinancialControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BankTransactionService bankTransactionService;

    @MockitoBean
    private BankCategoryRuleService bankCategoryRuleService;

    @Test
    void bindsTheFiltersAndPagingOfTheTransactionList() throws Exception {
        when(this.bankTransactionService.getTransactions(any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(new PageDTO<>(List.of(BankTransactionDTO.builder().id(7L).amount(new BigDecimal("-275")).build()), 41));

        this.mockMvc.perform(get("/api/v1/financial/transaction")
                        .param("search", "blue mart")
                        .param("flag", "COUNTED")
                        .param("categoryId", "0")
                        .param("from", "2025-12-01")
                        .param("to", "2025-12-31")
                        .param("page", "2")
                        .param("size", "50")
                        .param("sort", "amount")
                        .param("dir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(41))
                .andExpect(jsonPath("$.content[0].id").value(7));

        verify(this.bankTransactionService).getTransactions(any(), eq(new BankTransactionQuery("blue mart",
                        BankTransactionFlag.COUNTED, 0L, LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31))),
                eq(2), eq(50), eq("amount"), eq("asc"));
    }

    @Test
    void defaultsToTheFirstPageNewestFirst() throws Exception {
        when(this.bankTransactionService.getTransactions(any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(new PageDTO<>(List.of(), 0));

        this.mockMvc.perform(get("/api/v1/financial/transaction")).andExpect(status().isOk());

        verify(this.bankTransactionService).getTransactions(any(), eq(BankTransactionQuery.none()),
                eq(0), eq(25), eq("bookingDate"), eq("desc"));
    }

    @Test
    void returnsTheCategoryRuleMatchCounts() throws Exception {
        when(this.bankCategoryRuleService.countMatches(any(), eq("blue mart"))).thenReturn(new CategoryMatchDTO(3, 1));

        this.mockMvc.perform(get("/api/v1/financial/rule/category/match").param("pattern", "blue mart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches").value(3))
                .andExpect(jsonPath("$.uncategorized").value(1));
    }
}
