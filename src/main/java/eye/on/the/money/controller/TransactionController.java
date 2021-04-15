package eye.on.the.money.controller;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.TransactionDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("transaction")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    private static final Logger log = LoggerFactory.getLogger(EotmApplication.class);

    @GetMapping()
    public ResponseEntity<List<TransactionDTO>> getCoinTransactionsByUserId(@AuthenticationPrincipal User user){
        log.debug("Test debug log");
        return new ResponseEntity<List<TransactionDTO>>(this.transactionService.getTransactionsByUserId(user.getId()), HttpStatus.OK);
    }
}
