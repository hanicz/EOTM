package eye.on.the.money.controller;

import eye.on.the.money.dto.InvestmentDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.InvestmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("investment")
public class InvestmentController {

    @Autowired
    private InvestmentService investmentService;

    private static final Logger log = LoggerFactory.getLogger(InvestmentController.class);

    @GetMapping()
    public ResponseEntity<List<InvestmentDTO>> getStockInvestmentsByUserId(@AuthenticationPrincipal User user) {
        log.trace("Enter getStockInvestmentsByUserId");
        return new ResponseEntity<List<InvestmentDTO>>(this.investmentService.getInvestmentsByUserId(user.getId()), HttpStatus.OK);
    }

    @GetMapping("/{currency}")
    public ResponseEntity<List<InvestmentDTO>> getStockInvestmentsByUserIdAWConvCurr(@AuthenticationPrincipal User user, @PathVariable String currency) {
        log.trace("Enter getStockInvestmentsByUserIdAWConvCurr");
        return new ResponseEntity<List<InvestmentDTO>>(this.investmentService.getInvestmentsByUserIdWConvCurr(user.getId(), currency), HttpStatus.OK);
    }
}
