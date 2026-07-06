package eye.on.the.money.controller;


import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.service.etf.ETFInvestmentService;
import eye.on.the.money.util.CsvResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import eye.on.the.money.security.CurrentUserEmail;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/v1/etf")
@Slf4j
public class ETFController {

    private final ETFInvestmentService etfInvestmentService;

    @Autowired
    public ETFController(ETFInvestmentService etfInvestmentService) {
        this.etfInvestmentService = etfInvestmentService;
    }

    @GetMapping()
    public ResponseEntity<List<ETFInvestmentDTO>> getAllETFInvestments(@CurrentUserEmail String userEmail) {
        log.trace("Enter getAllETFInvestments");
        return ResponseEntity.ok(this.etfInvestmentService.getETFInvestments(userEmail));
    }

    @GetMapping("/holding")
    public ResponseEntity<List<ETFInvestmentDTO>> getETFHoldings(@CurrentUserEmail String userEmail) {
        log.trace("Enter getETFHoldings");
        return ResponseEntity.ok(this.etfInvestmentService.getCurrentETFHoldings(userEmail));
    }

    @GetMapping("/position")
    public ResponseEntity<List<ETFInvestmentDTO>> getPositions(@CurrentUserEmail String userEmail) {
        log.trace("Enter");
        return ResponseEntity.ok(this.etfInvestmentService.getAllPositions(userEmail));
    }

    @PostMapping
    public ResponseEntity<ETFInvestmentDTO> createInvestment(@CurrentUserEmail String userEmail, @RequestBody ETFInvestmentDTO investmentDTO) {
        log.trace("Enter");
        return ResponseEntity.status(HttpStatus.CREATED).body(this.etfInvestmentService.createInvestment(investmentDTO, userEmail));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserEmail String userEmail, @RequestParam List<Long> ids) {
        log.trace("Enter");
        this.etfInvestmentService.deleteInvestmentById(userEmail, ids);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/csv")
    public void getCSV(@CurrentUserEmail String userEmail, HttpServletResponse servletResponse) throws IOException {
        log.trace("Enter");
        this.etfInvestmentService.getCSV(userEmail, CsvResponseUtil.prepare(servletResponse, "etf_investments.csv"));
    }

    @PutMapping
    public ResponseEntity<ETFInvestmentDTO> updateInvestment(@CurrentUserEmail String userEmail, @RequestBody ETFInvestmentDTO investmentDTO) {
        log.trace("Enter");
        return ResponseEntity.ok(this.etfInvestmentService.updateInvestment(investmentDTO, userEmail));
    }

    @PostMapping("/process/csv")
    public ResponseEntity<Void> processCSV(@CurrentUserEmail String userEmail, @RequestParam("file") MultipartFile file) throws IOException {
        this.etfInvestmentService.processCSV(userEmail, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
