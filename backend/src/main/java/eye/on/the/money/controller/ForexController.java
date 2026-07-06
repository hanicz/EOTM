package eye.on.the.money.controller;

import eye.on.the.money.dto.out.ForexTransactionDTO;
import eye.on.the.money.service.forex.ForexTransactionService;
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
@RequestMapping("api/v1/forex")
@Slf4j
@RequiredArgsConstructor
public class ForexController {

    private final ForexTransactionService forexTransactionService;

    @GetMapping()
    public ResponseEntity<List<ForexTransactionDTO>> getForexTransactionsByUserId(@CurrentUserEmail String userEmail) {
        return ResponseEntity.ok(this.forexTransactionService.getForexTransactionsByUserId(userEmail));
    }

    @GetMapping("/holding")
    public ResponseEntity<List<ForexTransactionDTO>> getForexHoldings(@CurrentUserEmail String userEmail) {
        return ResponseEntity.ok(this.forexTransactionService.getAllForexHoldings(userEmail));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserEmail String userEmail, @RequestParam List<Long> ids) {
        this.forexTransactionService.deleteForexTransactionById(userEmail, ids);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<ForexTransactionDTO> createTransaction(@CurrentUserEmail String userEmail, @RequestBody ForexTransactionDTO forexTransactionDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.forexTransactionService.createForexTransaction(forexTransactionDTO, userEmail));
    }

    @PutMapping
    public ResponseEntity<ForexTransactionDTO> updateTransaction(@CurrentUserEmail String userEmail, @RequestBody ForexTransactionDTO forexTransactionDTO) {
        return ResponseEntity.ok(this.forexTransactionService.updateForexTransaction(forexTransactionDTO, userEmail));
    }

    @GetMapping("/csv")
    public void getCSV(@CurrentUserEmail String userEmail, HttpServletResponse servletResponse) throws IOException {
        this.forexTransactionService.getCSV(userEmail, CsvResponseUtil.prepare(servletResponse, "forex_transactions.csv"));
    }

    @PostMapping("/process/csv")
    public ResponseEntity<Void> processCSV(@CurrentUserEmail String userEmail, @RequestParam("file") MultipartFile file) throws IOException {
        this.forexTransactionService.processCSV(userEmail, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
