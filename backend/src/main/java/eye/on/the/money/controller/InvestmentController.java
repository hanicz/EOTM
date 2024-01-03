package eye.on.the.money.controller;

import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.stock.InvestmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("investment")
@Slf4j
public class InvestmentController {

    @Autowired
    private InvestmentService investmentService;

    @GetMapping()
    public ResponseEntity<List<InvestmentDTO>> getAllInvestments(@AuthenticationPrincipal User user) {
        log.trace("Enter getStockInvestmentsByUserId");
        return new ResponseEntity<>(this.investmentService.getInvestments(user.getId()), HttpStatus.OK);
    }

    @GetMapping("/holding")
    public ResponseEntity<List<InvestmentDTO>> getHoldings(@AuthenticationPrincipal User user) {
        log.trace("Enter getHoldings");
        return new ResponseEntity<>(this.investmentService.getCurrentHoldings(user.getId()), HttpStatus.OK);
    }

    @GetMapping("/position")
    public ResponseEntity<List<InvestmentDTO>> getPositions(@AuthenticationPrincipal User user) {
        log.trace("Enter getPositions");
        return new ResponseEntity<>(this.investmentService.getAllPositions(user.getId()), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<InvestmentDTO> createInvestment(@AuthenticationPrincipal User user, @RequestBody InvestmentDTO investmentDTO) {
        log.trace("Enter createInvestment");
        return new ResponseEntity<>(this.investmentService.createInvestment(investmentDTO, user), HttpStatus.CREATED);
    }

    @DeleteMapping()
    public ResponseEntity<HttpStatus> deleteByIds(@AuthenticationPrincipal User user, @RequestParam String ids) {
        log.trace("Enter deleteByIds");
        List<Long> idList = Stream.of(ids.split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        this.investmentService.deleteInvestmentById(user, idList);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/csv")
    public void getCSV(@AuthenticationPrincipal User user, HttpServletResponse servletResponse) throws IOException {
        log.trace("Enter getCSV");
        servletResponse.setContentType("text/csv");
        servletResponse.addHeader("Content-Disposition", "attachment; filename=\"investments.csv\"");
        this.investmentService.getCSV(user.getId(), servletResponse.getWriter());
    }

    @PutMapping
    public ResponseEntity<InvestmentDTO> updateInvestment(@AuthenticationPrincipal User user, @RequestBody InvestmentDTO investmentDTO) {
        log.trace("Enter updateInvestment");
        return new ResponseEntity<>(this.investmentService.updateInvestment(investmentDTO, user), HttpStatus.CREATED);
    }

    @PostMapping("/process/csv")
    public ResponseEntity<HttpStatus> processCSV(@AuthenticationPrincipal User user, @RequestParam("file") MultipartFile file) throws IOException {
        log.trace("Enter processCSV");
        this.investmentService.processCSV(user, file);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
