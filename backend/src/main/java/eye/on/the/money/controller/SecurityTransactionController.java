package eye.on.the.money.controller;

import eye.on.the.money.dto.out.SecurityTransactionDTO;
import eye.on.the.money.service.security.SecurityTransactionService;
import eye.on.the.money.util.CsvResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import eye.on.the.money.security.CurrentUserId;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/v1/security/transaction")
@Slf4j
@RequiredArgsConstructor
public class SecurityTransactionController {

    private final SecurityTransactionService securityTransactionService;

    @GetMapping()
    public ResponseEntity<List<SecurityTransactionDTO>> getAllTransactions(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.securityTransactionService.getTransactions(userId));
    }

    @GetMapping("/holding")
    public ResponseEntity<List<SecurityTransactionDTO>> getHoldings(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.securityTransactionService.getCurrentHoldings(userId));
    }

    @PostMapping
    public ResponseEntity<SecurityTransactionDTO> createTransaction(@CurrentUserId Long userId, @RequestBody SecurityTransactionDTO transactionDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.securityTransactionService.createTransaction(transactionDTO, userId));
    }

    @PutMapping
    public ResponseEntity<SecurityTransactionDTO> updateTransaction(@CurrentUserId Long userId, @RequestBody SecurityTransactionDTO transactionDTO) {
        return ResponseEntity.ok(this.securityTransactionService.updateTransaction(transactionDTO, userId));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserId Long userId, @RequestParam List<Long> ids) {
        this.securityTransactionService.deleteTransactionById(userId, ids);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/csv")
    public void getCSV(@CurrentUserId Long userId, HttpServletResponse servletResponse) throws IOException {
        this.securityTransactionService.getCSV(userId, CsvResponseUtil.prepare(servletResponse, "security_transactions.csv"));
    }

    @PostMapping("/process/csv")
    public ResponseEntity<Void> processCSV(@CurrentUserId Long userId, @RequestParam("file") MultipartFile file) throws IOException {
        this.securityTransactionService.processCSV(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
