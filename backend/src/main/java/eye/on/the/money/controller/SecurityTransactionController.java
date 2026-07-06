package eye.on.the.money.controller;

import eye.on.the.money.dto.out.SecurityTransactionDTO;
import eye.on.the.money.service.security.SecurityTransactionService;
import eye.on.the.money.util.CsvResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import eye.on.the.money.security.CurrentUserEmail;
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
    public ResponseEntity<List<SecurityTransactionDTO>> getAllTransactions(@CurrentUserEmail String userEmail) {
        return ResponseEntity.ok(this.securityTransactionService.getTransactions(userEmail));
    }

    @GetMapping("/holding")
    public ResponseEntity<List<SecurityTransactionDTO>> getHoldings(@CurrentUserEmail String userEmail) {
        return ResponseEntity.ok(this.securityTransactionService.getCurrentHoldings(userEmail));
    }

    @PostMapping
    public ResponseEntity<SecurityTransactionDTO> createTransaction(@CurrentUserEmail String userEmail, @RequestBody SecurityTransactionDTO transactionDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.securityTransactionService.createTransaction(transactionDTO, userEmail));
    }

    @PutMapping
    public ResponseEntity<SecurityTransactionDTO> updateTransaction(@CurrentUserEmail String userEmail, @RequestBody SecurityTransactionDTO transactionDTO) {
        return ResponseEntity.ok(this.securityTransactionService.updateTransaction(transactionDTO, userEmail));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserEmail String userEmail, @RequestParam List<Long> ids) {
        this.securityTransactionService.deleteTransactionById(userEmail, ids);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/csv")
    public void getCSV(@CurrentUserEmail String userEmail, HttpServletResponse servletResponse) throws IOException {
        this.securityTransactionService.getCSV(userEmail, CsvResponseUtil.prepare(servletResponse, "security_transactions.csv"));
    }

    @PostMapping("/process/csv")
    public ResponseEntity<Void> processCSV(@CurrentUserEmail String userEmail, @RequestParam("file") MultipartFile file) throws IOException {
        this.securityTransactionService.processCSV(userEmail, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
