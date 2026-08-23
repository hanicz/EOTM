package eye.on.the.money.controller;

import eye.on.the.money.dto.in.BankTransactionMemoDTO;
import eye.on.the.money.dto.out.BankTransactionDTO;
import eye.on.the.money.dto.out.ImportResultDTO;
import eye.on.the.money.dto.out.MonthlyCashFlowDTO;
import eye.on.the.money.dto.out.MonthlyIncomeDTO;
import eye.on.the.money.security.CurrentUserEmail;
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
    public ResponseEntity<List<BankTransactionDTO>> getAllTransactions(@CurrentUserEmail String userEmail) {
        return ResponseEntity.ok(this.bankTransactionService.getTransactions(userEmail));
    }

    @GetMapping("/report/monthly")
    public ResponseEntity<List<MonthlyCashFlowDTO>> getMonthlyCashFlow(@CurrentUserEmail String userEmail) {
        return ResponseEntity.ok(this.bankTransactionService.getMonthlyCashFlow(userEmail));
    }

    @GetMapping("/report/monthly/csv")
    public void getMonthlyCashFlowCSV(@CurrentUserEmail String userEmail, HttpServletResponse servletResponse) throws IOException {
        this.bankTransactionService.getMonthlyCashFlowCSV(userEmail,
                CsvResponseUtil.prepare(servletResponse, "monthly_cash_flow.csv"));
    }

    @GetMapping("/report/income")
    public ResponseEntity<List<MonthlyIncomeDTO>> getMonthlyIncome(@CurrentUserEmail String userEmail) {
        return ResponseEntity.ok(this.bankTransactionService.getMonthlyIncome(userEmail));
    }

    @GetMapping("/report/income/csv")
    public void getMonthlyIncomeCSV(@CurrentUserEmail String userEmail, HttpServletResponse servletResponse) throws IOException {
        this.bankTransactionService.getMonthlyIncomeCSV(userEmail,
                CsvResponseUtil.prepare(servletResponse, "monthly_income.csv"));
    }

    @PutMapping("/exclusion")
    public ResponseEntity<Void> setExcluded(@CurrentUserEmail String userEmail, @RequestParam List<Long> ids,
                                            @RequestParam boolean excluded) {
        this.bankTransactionService.setExcluded(userEmail, ids, excluded);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/taxable")
    public ResponseEntity<Void> setTaxable(@CurrentUserEmail String userEmail, @RequestParam List<Long> ids,
                                           @RequestParam boolean taxable) {
        this.taxableEventService.setTaxable(userEmail, ids, taxable);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/memo")
    public ResponseEntity<Void> updateMemo(@CurrentUserEmail String userEmail, @PathVariable Long id,
                                           @RequestBody @Valid BankTransactionMemoDTO memoDTO) {
        this.bankTransactionService.updateMemo(userEmail, id, memoDTO.memo());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserEmail String userEmail, @RequestParam List<Long> ids) {
        this.bankTransactionService.deleteTransactionById(userEmail, ids);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/csv")
    public void getCSV(@CurrentUserEmail String userEmail, HttpServletResponse servletResponse) throws IOException {
        this.bankTransactionService.getCSV(userEmail, CsvResponseUtil.prepare(servletResponse, "bank_transactions.csv"));
    }

    @PostMapping("/process/csv")
    public ResponseEntity<ImportResultDTO> processCSV(@CurrentUserEmail String userEmail, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(this.bankTransactionService.processCSV(userEmail, file));
    }
}
