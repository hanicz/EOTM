package eye.on.the.money.controller;

import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.service.crypto.TransactionService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("transaction")
@Slf4j
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

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
    public ResponseEntity<HttpStatus> deleteByIds(@AuthenticationPrincipal UserDetails user, @RequestParam String ids) {
        log.trace("Enter");
        List<Long> idList = Stream.of(ids.split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        this.transactionService.deleteTransactionById(user.getUsername(), idList);
        return new ResponseEntity<>(HttpStatus.OK);
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
