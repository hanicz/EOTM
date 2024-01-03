package eye.on.the.money.controller;


import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.etf.ETFInvestmentService;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("etf")
@Slf4j
public class ETFController {

    @Autowired
    private ETFInvestmentService etfInvestmentService;

    @GetMapping()
    public ResponseEntity<List<ETFInvestmentDTO>> getAllETFInvestments(@AuthenticationPrincipal User user) {
        log.trace("Enter getAllETFInvestments");
        return new ResponseEntity<>(this.etfInvestmentService.getETFInvestments(user.getId()), HttpStatus.OK);
    }

    @GetMapping("/holding")
    public ResponseEntity<List<ETFInvestmentDTO>> getETFHoldings(@AuthenticationPrincipal User user) {
        log.trace("Enter getETFHoldings");
        return new ResponseEntity<>(this.etfInvestmentService.getCurrentETFHoldings(user.getId()), HttpStatus.OK);
    }

    @GetMapping("/position")
    public ResponseEntity<List<ETFInvestmentDTO>> getPositions(@AuthenticationPrincipal User user) {
        log.trace("Enter getPositions");
        return new ResponseEntity<>(this.etfInvestmentService.getAllPositions(user.getId()), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ETFInvestmentDTO> createInvestment(@AuthenticationPrincipal User user, @RequestBody ETFInvestmentDTO investmentDTO) {
        log.trace("Enter createInvestment");
        return new ResponseEntity<>(this.etfInvestmentService.createInvestment(investmentDTO, user), HttpStatus.CREATED);
    }

    @DeleteMapping()
    public ResponseEntity<HttpStatus> deleteByIds(@AuthenticationPrincipal User user, @RequestParam String ids) {
        log.trace("Enter deleteByIds");
        List<Long> idList = Stream.of(ids.split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        this.etfInvestmentService.deleteInvestmentById(user, idList);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/csv")
    public void getCSV(@AuthenticationPrincipal User user, HttpServletResponse servletResponse) throws IOException {
        log.trace("Enter getCSV");
        servletResponse.setContentType("text/csv");
        servletResponse.addHeader("Content-Disposition", "attachment; filename=\"investments.csv\"");
        this.etfInvestmentService.getCSV(user.getId(), servletResponse.getWriter());
    }

    @PutMapping
    public ResponseEntity<ETFInvestmentDTO> updateInvestment(@AuthenticationPrincipal User user, @RequestBody ETFInvestmentDTO investmentDTO) {
        log.trace("Enter updateInvestment");
        return new ResponseEntity<>(this.etfInvestmentService.updateInvestment(investmentDTO, user), HttpStatus.CREATED);
    }
}
