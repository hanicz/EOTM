package eye.on.the.money.controller;

import eye.on.the.money.dto.out.SecurityTransactionDTO;
import eye.on.the.money.service.security.SecurityTransactionService;
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
@RequestMapping("api/v1/security/transaction")
@Slf4j
@RequiredArgsConstructor
public class SecurityTransactionController {

    private final SecurityTransactionService securityTransactionService;

    @GetMapping()
    public ResponseEntity<List<SecurityTransactionDTO>> getAllTransactions(@AuthenticationPrincipal UserDetails user) {
        return new ResponseEntity<>(this.securityTransactionService.getTransactions(user.getUsername()), HttpStatus.OK);
    }

    @GetMapping("/holding")
    public ResponseEntity<List<SecurityTransactionDTO>> getHoldings(@AuthenticationPrincipal UserDetails user) {
        return new ResponseEntity<>(this.securityTransactionService.getCurrentHoldings(user.getUsername()), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<SecurityTransactionDTO> createTransaction(@AuthenticationPrincipal UserDetails user, @RequestBody SecurityTransactionDTO transactionDTO) {
        return new ResponseEntity<>(this.securityTransactionService.createTransaction(transactionDTO, user.getUsername()), HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<SecurityTransactionDTO> updateTransaction(@AuthenticationPrincipal UserDetails user, @RequestBody SecurityTransactionDTO transactionDTO) {
        return new ResponseEntity<>(this.securityTransactionService.updateTransaction(transactionDTO, user.getUsername()), HttpStatus.OK);
    }

    @DeleteMapping()
    public ResponseEntity<HttpStatus> deleteByIds(@AuthenticationPrincipal UserDetails user, @RequestParam List<Long> ids) {
        this.securityTransactionService.deleteTransactionById(user.getUsername(), ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/csv")
    public void getCSV(@AuthenticationPrincipal UserDetails user, HttpServletResponse servletResponse) throws IOException {
        servletResponse.setContentType("text/csv");
        servletResponse.addHeader("Content-Disposition", "attachment; filename=\"security_transactions.csv\"");
        this.securityTransactionService.getCSV(user.getUsername(), servletResponse.getWriter());
    }

    @PostMapping("/process/csv")
    public ResponseEntity<HttpStatus> processCSV(@AuthenticationPrincipal UserDetails user, @RequestParam("file") MultipartFile file) throws IOException {
        this.securityTransactionService.processCSV(user.getUsername(), file);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
