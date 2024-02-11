package eye.on.the.money.controller;

import eye.on.the.money.dto.out.ForexTransactionDTO;
import eye.on.the.money.service.forex.ForexTransactionService;
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
@RequestMapping("api/v1/forex")
@Slf4j
@RequiredArgsConstructor
public class ForexController {

    private final ForexTransactionService forexTransactionService;

    @GetMapping()
    public ResponseEntity<List<ForexTransactionDTO>> getForexTransactionsByUserId(@AuthenticationPrincipal UserDetails user) {
        return new ResponseEntity<>(this.forexTransactionService.getForexTransactionsByUserId(user.getUsername()), HttpStatus.OK);
    }

    @GetMapping("/holding")
    public ResponseEntity<List<ForexTransactionDTO>> getForexHoldings(@AuthenticationPrincipal UserDetails user) {
        return new ResponseEntity<>(this.forexTransactionService.getAllForexHoldings(user.getUsername()), HttpStatus.OK);
    }

    @DeleteMapping()
    public ResponseEntity<HttpStatus> deleteByIds(@AuthenticationPrincipal UserDetails user, @RequestParam String ids) {
        this.forexTransactionService.deleteForexTransactionById(user.getUsername(), ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ForexTransactionDTO> createTransaction(@AuthenticationPrincipal UserDetails user, @RequestBody ForexTransactionDTO forexTransactionDTO) {
        return new ResponseEntity<>(this.forexTransactionService.createForexTransaction(forexTransactionDTO, user.getUsername()), HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<ForexTransactionDTO> updateTransaction(@AuthenticationPrincipal UserDetails user, @RequestBody ForexTransactionDTO forexTransactionDTO) {
        return new ResponseEntity<>(this.forexTransactionService.updateForexTransaction(forexTransactionDTO, user.getUsername()), HttpStatus.OK);
    }

    @GetMapping("/csv")
    public void getCSV(@AuthenticationPrincipal UserDetails user, HttpServletResponse servletResponse) throws IOException {
        servletResponse.setContentType("text/csv");
        servletResponse.addHeader("Content-Disposition", "attachment; filename=\"investments.csv\"");
        this.forexTransactionService.getCSV(user.getUsername(), servletResponse.getWriter());
    }

    @PostMapping("/process/csv")
    public ResponseEntity<HttpStatus> processCSV(@AuthenticationPrincipal UserDetails user, @RequestParam("file") MultipartFile file) throws IOException {
        this.forexTransactionService.processCSV(user.getUsername(), file);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
