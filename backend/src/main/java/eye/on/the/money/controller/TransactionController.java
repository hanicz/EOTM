package eye.on.the.money.controller;

import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.service.crypto.TransactionService;
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
@RequestMapping("api/v1/transaction")
@Slf4j
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping()
    public ResponseEntity<List<TransactionDTO>> getCoinTransactionsByUserId(@CurrentUserEmail String userEmail) {
        log.trace("Enter getCoinTransactionsByUserId");
        return ResponseEntity.ok(this.transactionService.getTransactionsByUserId(userEmail));
    }

    @GetMapping("/position")
    public ResponseEntity<List<TransactionDTO>> getAllPositions(@CurrentUserEmail String userEmail) {
        log.trace("Enter");
        return ResponseEntity.ok(this.transactionService.getAllPositions(userEmail));
    }

    @PostMapping("/holding")
    public ResponseEntity<List<TransactionDTO>> getAllHoldings(@CurrentUserEmail String userEmail, @RequestBody TransactionQuery query) {
        log.trace("Enter");
        return ResponseEntity.ok(this.transactionService.getCurrentHoldings(userEmail, query));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserEmail String userEmail, @RequestParam List<Long> ids) {
        var isDeleted = this.transactionService.deleteTransactionById(userEmail, ids);
        return ResponseEntity.status(isDeleted ? HttpStatus.OK : HttpStatus.NOT_FOUND).build();
    }

    @GetMapping("/csv")
    public void getCSV(@CurrentUserEmail String userEmail, HttpServletResponse servletResponse) throws IOException {
        log.trace("Enter");
        this.transactionService.getCSV(userEmail, CsvResponseUtil.prepare(servletResponse, "transactions.csv"));
    }

    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@CurrentUserEmail String userEmail, @RequestBody TransactionDTO transactionDTO) {
        log.trace("Enter");
        return ResponseEntity.status(HttpStatus.CREATED).body(this.transactionService.createTransaction(transactionDTO, userEmail));
    }

    @PutMapping
    public ResponseEntity<TransactionDTO> updateTransaction(@CurrentUserEmail String userEmail, @RequestBody TransactionDTO transactionDTO) {
        log.trace("Enter");
        return ResponseEntity.ok(this.transactionService.updateTransaction(transactionDTO, userEmail));
    }

    @PostMapping("/process/csv")
    public ResponseEntity<Void> processCSV(@CurrentUserEmail String userEmail, @RequestParam("file") MultipartFile file) throws IOException {
        log.trace("Enter");
        this.transactionService.processCSV(userEmail, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
