package eye.on.the.money.controller;


import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.service.etf.ETFInvestmentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("etf")
@Slf4j
public class ETFController {

    private final ETFInvestmentService etfInvestmentService;

    @Autowired
    public ETFController(ETFInvestmentService etfInvestmentService) {
        this.etfInvestmentService = etfInvestmentService;
    }

    @GetMapping()
    public ResponseEntity<List<ETFInvestmentDTO>> getAllETFInvestments(@AuthenticationPrincipal UserDetails user) {
        log.trace("Enter getAllETFInvestments");
        return new ResponseEntity<>(this.etfInvestmentService.getETFInvestments(user.getUsername()), HttpStatus.OK);
    }

    @GetMapping("/holding")
    public ResponseEntity<List<ETFInvestmentDTO>> getETFHoldings(@AuthenticationPrincipal UserDetails user) {
        log.trace("Enter getETFHoldings");
        return new ResponseEntity<>(this.etfInvestmentService.getCurrentETFHoldings(user.getUsername()), HttpStatus.OK);
    }

    @GetMapping("/position")
    public ResponseEntity<List<ETFInvestmentDTO>> getPositions(@AuthenticationPrincipal UserDetails user) {
        log.trace("Enter");
        return new ResponseEntity<>(this.etfInvestmentService.getAllPositions(user.getUsername()), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ETFInvestmentDTO> createInvestment(@AuthenticationPrincipal UserDetails user, @RequestBody ETFInvestmentDTO investmentDTO) {
        log.trace("Enter");
        return new ResponseEntity<>(this.etfInvestmentService.createInvestment(investmentDTO, user.getUsername()), HttpStatus.CREATED);
    }

    @DeleteMapping()
    public ResponseEntity<HttpStatus> deleteByIds(@AuthenticationPrincipal UserDetails user, @RequestParam String ids) {
        log.trace("Enter");
        List<Long> idList = Stream.of(ids.split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        this.etfInvestmentService.deleteInvestmentById(user.getUsername(), idList);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/csv")
    public void getCSV(@AuthenticationPrincipal UserDetails user, HttpServletResponse servletResponse) throws IOException {
        log.trace("Enter");
        servletResponse.setContentType("text/csv");
        servletResponse.addHeader("Content-Disposition", "attachment; filename=\"investments.csv\"");
        this.etfInvestmentService.getCSV(user.getUsername(), servletResponse.getWriter());
    }

    @PutMapping
    public ResponseEntity<ETFInvestmentDTO> updateInvestment(@AuthenticationPrincipal UserDetails user, @RequestBody ETFInvestmentDTO investmentDTO) {
        log.trace("Enter");
        return new ResponseEntity<>(this.etfInvestmentService.updateInvestment(investmentDTO, user.getUsername()), HttpStatus.CREATED);
    }
}
