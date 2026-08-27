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
import eye.on.the.money.security.CurrentUserId;
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
    public ResponseEntity<List<TransactionDTO>> getCoinTransactionsByUserId(@CurrentUserId Long userId) {
        log.trace("Enter getCoinTransactionsByUserId");
        return ResponseEntity.ok(this.transactionService.getTransactionsByUserId(userId));
    }

    @GetMapping("/position")
    public ResponseEntity<List<TransactionDTO>> getAllPositions(@CurrentUserId Long userId) {
        log.trace("Enter");
        return ResponseEntity.ok(this.transactionService.getAllPositions(userId));
    }

    @PostMapping("/holding")
    public ResponseEntity<List<TransactionDTO>> getAllHoldings(@CurrentUserId Long userId,
                                                               @RequestBody TransactionQuery query,
                                                               @RequestParam(defaultValue = "false") boolean refresh) {
        log.trace("Enter");
        return ResponseEntity.ok(refresh ? this.transactionService.refreshCurrentHoldings(userId, query)
                : this.transactionService.getCurrentHoldings(userId, query));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserId Long userId, @RequestParam List<Long> ids) {
        this.transactionService.deleteTransactionById(userId, ids);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/csv")
    public void getCSV(@CurrentUserId Long userId, HttpServletResponse servletResponse) throws IOException {
        log.trace("Enter");
        this.transactionService.getCSV(userId, CsvResponseUtil.prepare(servletResponse, "transactions.csv"));
    }

    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@CurrentUserId Long userId, @RequestBody TransactionDTO transactionDTO) {
        log.trace("Enter");
        return ResponseEntity.status(HttpStatus.CREATED).body(this.transactionService.createTransaction(transactionDTO, userId));
    }

    @PutMapping
    public ResponseEntity<TransactionDTO> updateTransaction(@CurrentUserId Long userId, @RequestBody TransactionDTO transactionDTO) {
        log.trace("Enter");
        return ResponseEntity.ok(this.transactionService.updateTransaction(transactionDTO, userId));
    }

    @PostMapping("/process/csv")
    public ResponseEntity<Void> processCSV(@CurrentUserId Long userId, @RequestParam("file") MultipartFile file) throws IOException {
        log.trace("Enter");
        this.transactionService.processCSV(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
