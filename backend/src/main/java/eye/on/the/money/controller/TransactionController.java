package eye.on.the.money.controller;

import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.service.crypto.TransactionService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    public ResponseEntity<List<TransactionDTO>> getCoinTransactionsByUserId(@AuthenticationPrincipal UserDetails user) {
        log.trace("Enter getCoinTransactionsByUserId");
        return new ResponseEntity<>(this.transactionService.getTransactionsByUserId(user.getUsername()), HttpStatus.OK);
    }

    @GetMapping("/position")
    public ResponseEntity<List<TransactionDTO>> getAllPositions(@AuthenticationPrincipal UserDetails user) {
        log.trace("Enter");
        return new ResponseEntity<>(this.transactionService.getAllPositions(user.getUsername()), HttpStatus.OK);
    }

    @PostMapping("/holding")
    public ResponseEntity<List<TransactionDTO>> getAllHoldings(@AuthenticationPrincipal UserDetails user, @RequestBody TransactionQuery query) {
        log.trace("Enter");
        return new ResponseEntity<>(this.transactionService.getCurrentHoldings(user.getUsername(), query), HttpStatus.OK);
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@AuthenticationPrincipal UserDetails user, @RequestParam String ids) {
        var isDeleted = this.transactionService.deleteTransactionById(user.getUsername(), ids);
        return new ResponseEntity<>(isDeleted ? HttpStatus.OK : HttpStatus.NOT_FOUND);
    }

    @GetMapping("/csv")
    public void getCSV(@AuthenticationPrincipal UserDetails user, HttpServletResponse servletResponse) throws IOException {
        log.trace("Enter");
        servletResponse.setContentType("text/csv");
        servletResponse.addHeader("Content-Disposition", "attachment; filename=\"transactions.csv\"");
        this.transactionService.getCSV(user.getUsername(), servletResponse.getWriter());
    }

    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@AuthenticationPrincipal UserDetails user, @RequestBody TransactionDTO transactionDTO) {
        log.trace("Enter");
        return new ResponseEntity<>(this.transactionService.createTransaction(transactionDTO, user.getUsername()), HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<TransactionDTO> updateTransaction(@AuthenticationPrincipal UserDetails user, @RequestBody TransactionDTO transactionDTO) {
        log.trace("Enter");
        return new ResponseEntity<>(this.transactionService.updateTransaction(transactionDTO, user.getUsername()), HttpStatus.OK);
    }

    @PostMapping("/process/csv")
    public ResponseEntity<HttpStatus> processCSV(@AuthenticationPrincipal UserDetails user, @RequestParam("file") MultipartFile file) throws IOException {
        log.trace("Enter");
        this.transactionService.processCSV(user.getUsername(), file);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
