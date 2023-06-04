package eye.on.the.money.controller;

import eye.on.the.money.dto.out.ForexTransactionDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.forex.ForexTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("forex")
@Slf4j
public class ForexController {

    @Autowired
    private ForexTransactionService forexTransactionService;

    @GetMapping()
    public ResponseEntity<List<ForexTransactionDTO>> getForexTransactionsByUserId(@AuthenticationPrincipal User user) {
        log.trace("Enter getForexTransactionsByUserId");
        return new ResponseEntity<>(this.forexTransactionService.getForexTransactionsByUserId(user.getId()), HttpStatus.OK);
    }

    @GetMapping("/holding")
    public ResponseEntity<List<ForexTransactionDTO>> getForexHoldings(@AuthenticationPrincipal User user) {
        log.trace("Enter getForexHoldings");
        return new ResponseEntity<>(this.forexTransactionService.getAllForexHoldings(user.getId()), HttpStatus.OK);
    }

    @DeleteMapping()
    public ResponseEntity<HttpStatus> deleteByIds(@AuthenticationPrincipal User user, @RequestParam String ids) {
        log.trace("Enter deleteByIds");
        List<Long> idList = Stream.of(ids.split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        this.forexTransactionService.deleteForexTransactionById(user, idList);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping
    public ResponseEntity<ForexTransactionDTO> createTransaction(@AuthenticationPrincipal User user, @RequestBody ForexTransactionDTO forexTransactionDTO) {
        log.trace("Enter createTransaction");
        return new ResponseEntity<>(this.forexTransactionService.createForexTransaction(forexTransactionDTO, user), HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<ForexTransactionDTO> updateTransaction(@AuthenticationPrincipal User user, @RequestBody ForexTransactionDTO forexTransactionDTO) {
        log.trace("Enter updateTransaction");
        return new ResponseEntity<>(this.forexTransactionService.updateForexTransaction(forexTransactionDTO, user), HttpStatus.OK);
    }
}
