package eye.on.the.money.controller;

import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("transaction")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    private TransactionService transactionService;

    @GetMapping()
    public ResponseEntity<List<TransactionDTO>> getCoinTransactionsByUserId(@AuthenticationPrincipal User user){
        log.trace("Enter getCoinTransactionsByUserId");
        return new ResponseEntity<List<TransactionDTO>>(this.transactionService.getTransactionsByUserId(user.getId()), HttpStatus.OK);
    }

    @PostMapping("/currency")
    public ResponseEntity<List<TransactionDTO>> getCoinTransactionsByUserIdWCurr(@AuthenticationPrincipal User user,  @RequestBody TransactionQuery query){
        log.trace("Enter getCoinTransactionsByUserIdWCurr");
        return new ResponseEntity<List<TransactionDTO>>(this.transactionService.getTransactionsByUserIdWConvCurr(user.getId(), query.getCurrency()), HttpStatus.OK);
    }

    @PostMapping("/position")
    public ResponseEntity<List<TransactionDTO>> getAllPositions(@AuthenticationPrincipal User user, @RequestBody TransactionQuery query) {
        log.trace("Enter getAllPositions");
        return new ResponseEntity<List<TransactionDTO>>(this.transactionService.getAllPositions(user.getId(), query), HttpStatus.OK);
    }

    @PostMapping("/holding")
    public ResponseEntity<List<TransactionDTO>> getAllHoldings(@AuthenticationPrincipal User user, @RequestBody TransactionQuery query) {
        log.trace("Enter getAllHoldings");
        return new ResponseEntity<List<TransactionDTO>>(this.transactionService.getCurrentHoldings(user.getId(), query), HttpStatus.OK);
    }

    @DeleteMapping()
    public ResponseEntity<HttpStatus> deleteByIds(@AuthenticationPrincipal User user, @RequestParam  String ids) {
        log.trace("Enter deleteByIds");
        List<Long> idList = Stream.of(ids.split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        this.transactionService.deleteTransactionById(idList);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/csv")
    public void getCSV(@AuthenticationPrincipal User user, HttpServletResponse servletResponse) throws IOException {
        log.trace("Enter getCoinTransactionsByUserId");
        servletResponse.setContentType("text/csv");
        servletResponse.addHeader("Content-Disposition","attachment; filename=\"transactions.csv\"");
        this.transactionService.getCSV(user.getId(), servletResponse.getWriter());
    }
}
