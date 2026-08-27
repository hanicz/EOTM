package eye.on.the.money.controller;

import eye.on.the.money.dto.in.BankTransactionEditDTO;
import eye.on.the.money.dto.out.BankTransactionDTO;
import eye.on.the.money.dto.out.ImportResultDTO;
import eye.on.the.money.dto.out.MonthlyCashFlowDTO;
import eye.on.the.money.dto.out.MonthlyIncomeDTO;
import eye.on.the.money.security.CurrentUserId;
import eye.on.the.money.service.financial.BankTransactionService;
import eye.on.the.money.service.financial.TaxableEventService;
import eye.on.the.money.util.CsvResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/v1/financial/transaction")
@Slf4j
@Validated
@RequiredArgsConstructor
public class FinancialController {

    private final BankTransactionService bankTransactionService;
    private final TaxableEventService taxableEventService;

    @GetMapping()
    public ResponseEntity<List<BankTransactionDTO>> getAllTransactions(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.bankTransactionService.getTransactions(userId));
    }

    @GetMapping("/report/monthly")
    public ResponseEntity<List<MonthlyCashFlowDTO>> getMonthlyCashFlow(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.bankTransactionService.getMonthlyCashFlow(userId));
    }

    @GetMapping("/report/monthly/csv")
    public void getMonthlyCashFlowCSV(@CurrentUserId Long userId, HttpServletResponse servletResponse) throws IOException {
        this.bankTransactionService.getMonthlyCashFlowCSV(userId,
                CsvResponseUtil.prepare(servletResponse, "monthly_cash_flow.csv"));
    }

    @GetMapping("/report/income")
    public ResponseEntity<List<MonthlyIncomeDTO>> getMonthlyIncome(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.bankTransactionService.getMonthlyIncome(userId));
    }

    @GetMapping("/report/income/csv")
    public void getMonthlyIncomeCSV(@CurrentUserId Long userId, HttpServletResponse servletResponse) throws IOException {
        this.bankTransactionService.getMonthlyIncomeCSV(userId,
                CsvResponseUtil.prepare(servletResponse, "monthly_income.csv"));
    }

    @PutMapping("/exclusion")
    public ResponseEntity<Void> setExcluded(@CurrentUserId Long userId, @RequestParam List<Long> ids,
                                            @RequestParam boolean excluded) {
        this.bankTransactionService.setExcluded(userId, ids, excluded);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/taxable")
    public ResponseEntity<Void> setTaxable(@CurrentUserId Long userId, @RequestParam List<Long> ids,
                                           @RequestParam boolean taxable) {
        this.taxableEventService.setTaxable(userId, ids, taxable);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateTransaction(@CurrentUserId Long userId, @PathVariable Long id,
                                                  @RequestBody @Valid BankTransactionEditDTO editDTO) {
        this.bankTransactionService.updateTransaction(userId, id, editDTO);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserId Long userId, @RequestParam List<Long> ids) {
        this.bankTransactionService.deleteTransactionById(userId, ids);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/csv")
    public void getCSV(@CurrentUserId Long userId, HttpServletResponse servletResponse) throws IOException {
        this.bankTransactionService.getCSV(userId, CsvResponseUtil.prepare(servletResponse, "bank_transactions.csv"));
    }

    @PostMapping("/process/csv")
    public ResponseEntity<ImportResultDTO> processCSV(@CurrentUserId Long userId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(this.bankTransactionService.processCSV(userId, file));
    }
}
