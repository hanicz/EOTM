package eye.on.the.money.controller;

import eye.on.the.money.dto.out.ForexTransactionDTO;
import eye.on.the.money.service.forex.ForexTransactionService;
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
@RequestMapping("api/v1/forex")
@Slf4j
@RequiredArgsConstructor
public class ForexController {

    private final ForexTransactionService forexTransactionService;

    @GetMapping()
    public ResponseEntity<List<ForexTransactionDTO>> getForexTransactionsByUserId(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.forexTransactionService.getForexTransactionsByUserId(userId));
    }

    @GetMapping("/holding")
    public ResponseEntity<List<ForexTransactionDTO>> getForexHoldings(@CurrentUserId Long userId,
                                                                      @RequestParam(defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(refresh ? this.forexTransactionService.refreshAllForexHoldings(userId)
                : this.forexTransactionService.getAllForexHoldings(userId));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserId Long userId, @RequestParam List<Long> ids) {
        this.forexTransactionService.deleteForexTransactionById(userId, ids);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<ForexTransactionDTO> createTransaction(@CurrentUserId Long userId, @RequestBody ForexTransactionDTO forexTransactionDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.forexTransactionService.createForexTransaction(forexTransactionDTO, userId));
    }

    @PutMapping
    public ResponseEntity<ForexTransactionDTO> updateTransaction(@CurrentUserId Long userId, @RequestBody ForexTransactionDTO forexTransactionDTO) {
        return ResponseEntity.ok(this.forexTransactionService.updateForexTransaction(forexTransactionDTO, userId));
    }

    @GetMapping("/csv")
    public void getCSV(@CurrentUserId Long userId, HttpServletResponse servletResponse) throws IOException {
        this.forexTransactionService.getCSV(userId, CsvResponseUtil.prepare(servletResponse, "forex_transactions.csv"));
    }

    @PostMapping("/process/csv")
    public ResponseEntity<Void> processCSV(@CurrentUserId Long userId, @RequestParam("file") MultipartFile file) throws IOException {
        this.forexTransactionService.processCSV(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
