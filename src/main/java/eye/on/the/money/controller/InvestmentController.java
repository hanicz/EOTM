package eye.on.the.money.controller;

import eye.on.the.money.dto.in.InvestmentQuery;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.InvestmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("investment")
public class InvestmentController {

    @Autowired
    private InvestmentService investmentService;

    private static final Logger log = LoggerFactory.getLogger(InvestmentController.class);

    @GetMapping()
    public ResponseEntity<List<InvestmentDTO>> getAllInvestments(@AuthenticationPrincipal User user) {
        log.trace("Enter getStockInvestmentsByUserId");
        return new ResponseEntity<List<InvestmentDTO>>(this.investmentService.getInvestments(user.getId()), HttpStatus.OK);
    }

    @PostMapping("/currency")
    public ResponseEntity<List<InvestmentDTO>> getInvestmentsWCurr(@AuthenticationPrincipal User user, @RequestBody InvestmentQuery query) {
        log.trace("Enter getInvestmentsWCurr");
        return new ResponseEntity<List<InvestmentDTO>>(this.investmentService.getInvestmentsByUserIdWConvCurr(user.getId(), query.getCurrency()), HttpStatus.OK);
    }

    @GetMapping("/type")
    public ResponseEntity<List<InvestmentDTO>> getInvestmentsByBuySell(@AuthenticationPrincipal User user, @RequestBody InvestmentQuery query) {
        log.trace("Enter getInvestmentsByBuySell");
        return new ResponseEntity<List<InvestmentDTO>>(this.investmentService.getInvestmentsByBuySell(user.getId(), query), HttpStatus.OK);
    }

    @GetMapping("/date")
    public ResponseEntity<List<InvestmentDTO>> getInvestmentsByDate(@AuthenticationPrincipal User user, @RequestBody InvestmentQuery query) {
        log.trace("Enter getInvestmentsByDate");
        return new ResponseEntity<List<InvestmentDTO>>(this.investmentService.getInvestmentsByDate(user.getId(), query), HttpStatus.OK);
    }

    @GetMapping("/typeAndDate")
    public ResponseEntity<List<InvestmentDTO>> getInvestmentsByTypeAndDate(@AuthenticationPrincipal User user, @RequestBody InvestmentQuery query) {
        log.trace("Enter getInvestmentsByTypeAndDate");
        return new ResponseEntity<List<InvestmentDTO>>(this.investmentService.getInvestmentsByTypeAndDate(user.getId(), query), HttpStatus.OK);
    }

     @PostMapping("/holding")
    public ResponseEntity<List<InvestmentDTO>> getHoldings(@AuthenticationPrincipal User user, @RequestBody InvestmentQuery query) {
        log.trace("Enter getHoldings");
        return new ResponseEntity<List<InvestmentDTO>>(this.investmentService.getCurrentHoldings(user.getId(), query), HttpStatus.OK);
    }

    @PostMapping("/position")
    public ResponseEntity<List<InvestmentDTO>> getPositions(@AuthenticationPrincipal User user, @RequestBody InvestmentQuery query) {
        log.trace("Enter getPositions");
        return new ResponseEntity<List<InvestmentDTO>>(this.investmentService.getAllPositions(user.getId(), query), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<InvestmentDTO> createInvestment(@AuthenticationPrincipal User user, @RequestBody InvestmentDTO investmentDTO){
        log.trace("Enter createInvestment");
        return new ResponseEntity<InvestmentDTO>(this.investmentService.createInvestment(investmentDTO, user), HttpStatus.CREATED);
    }
}
