package eye.on.the.money.controller;

import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.crypto.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
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
    public ResponseEntity<List<TransactionDTO>> getCoinTransactionsByUserId(@AuthenticationPrincipal User user) {
        log.trace("Enter getCoinTransactionsByUserId");
        return new ResponseEntity<>(this.transactionService.getTransactionsByUserId(user.getId()), HttpStatus.OK);
    }

    @PostMapping("/position")
    public ResponseEntity<List<TransactionDTO>> getAllPositions(@AuthenticationPrincipal User user) {
        log.trace("Enter getAllPositions");
        return new ResponseEntity<>(this.transactionService.getAllPositions(user.getId()), HttpStatus.OK);
    }

    @PostMapping("/holding")
    public ResponseEntity<List<TransactionDTO>> getAllHoldings(@AuthenticationPrincipal User user, @RequestBody TransactionQuery query) {
        log.trace("Enter getAllHoldings");
        return new ResponseEntity<>(this.transactionService.getCurrentHoldings(user.getId(), query), HttpStatus.OK);
    }

    @DeleteMapping()
    public ResponseEntity<HttpStatus> deleteByIds(@AuthenticationPrincipal User user, @RequestParam String ids) {
        log.trace("Enter deleteByIds");
        List<Long> idList = Stream.of(ids.split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        this.transactionService.deleteTransactionById(user, idList);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/csv")
    public void getCSV(@AuthenticationPrincipal User user, HttpServletResponse servletResponse) throws IOException {
        log.trace("Enter getCSV");
        servletResponse.setContentType("text/csv");
        servletResponse.addHeader("Content-Disposition", "attachment; filename=\"transactions.csv\"");
        this.transactionService.getCSV(user.getId(), servletResponse.getWriter());
    }

    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@AuthenticationPrincipal User user, @RequestBody TransactionDTO transactionDTO) {
        log.trace("Enter createTransaction");
        return new ResponseEntity<>(this.transactionService.createTransaction(transactionDTO, user), HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<TransactionDTO> updateTransaction(@AuthenticationPrincipal User user, @RequestBody TransactionDTO transactionDTO) {
        log.trace("Enter updateTransaction");
        return new ResponseEntity<>(this.transactionService.updateTransaction(transactionDTO, user), HttpStatus.OK);
    }

    @PostMapping("/process/csv")
    public ResponseEntity<HttpStatus> processCSV(@AuthenticationPrincipal User user, @RequestParam("file") MultipartFile file) throws IOException {
        log.trace("Enter processCSV");
        this.transactionService.processCSV(user, file);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
