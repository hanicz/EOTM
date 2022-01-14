package eye.on.the.money.controller;


import eye.on.the.money.dto.in.InvestmentQuery;
import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.ETFInvestmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("etf")
public class ETFController {

    @Autowired
    private ETFInvestmentService etfInvestmentService;

    private static final Logger log = LoggerFactory.getLogger(ETFController.class);

    @GetMapping()
    public ResponseEntity<List<ETFInvestmentDTO>> getAllETFInvestments(@AuthenticationPrincipal User user) {
        log.trace("Enter getAllETFInvestments");
        return new ResponseEntity<List<ETFInvestmentDTO>>(this.etfInvestmentService.getETFInvestments(user.getId()), HttpStatus.OK);
    }

    @PostMapping("/holding")
    public ResponseEntity<List<ETFInvestmentDTO>> getETFHoldings(@AuthenticationPrincipal User user, @RequestBody InvestmentQuery query) {
        log.trace("Enter getETFHoldings");
        this.etfInvestmentService.updateETFPrices();
        return new ResponseEntity<List<ETFInvestmentDTO>>(this.etfInvestmentService.getCurrentETFHoldings(user.getId(), query), HttpStatus.OK);
    }
}
