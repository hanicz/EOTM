package eye.on.the.money.controller;


import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.service.etf.ETFInvestmentService;
import eye.on.the.money.util.CsvResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import eye.on.the.money.security.CurrentUserId;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/v1/etf")
public class ETFController {

    private final ETFInvestmentService etfInvestmentService;

    @Autowired
    public ETFController(ETFInvestmentService etfInvestmentService) {
        this.etfInvestmentService = etfInvestmentService;
    }

    @GetMapping()
    public ResponseEntity<List<ETFInvestmentDTO>> getAllETFInvestments(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.etfInvestmentService.getETFInvestments(userId));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<ETFInvestmentDTO>> getETFInvestmentsByAccount(@CurrentUserId Long userId, @PathVariable Long accountId) {
        return ResponseEntity.ok(this.etfInvestmentService.getETFInvestmentsByAccountId(userId, accountId));
    }

    @GetMapping("/holding")
    public ResponseEntity<List<ETFInvestmentDTO>> getETFHoldings(@CurrentUserId Long userId,
                                                                 @RequestParam(defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(refresh ? this.etfInvestmentService.refreshCurrentETFHoldings(userId)
                : this.etfInvestmentService.getCurrentETFHoldings(userId));
    }

    @GetMapping("/holding/account/{accountId}")
    public ResponseEntity<List<ETFInvestmentDTO>> getHoldingsByAccount(@CurrentUserId Long userId, @PathVariable Long accountId) {
        return ResponseEntity.ok(this.etfInvestmentService.getHoldingsByAccountId(userId, accountId));
    }

    @GetMapping("/position")
    public ResponseEntity<List<ETFInvestmentDTO>> getPositions(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.etfInvestmentService.getAllPositions(userId));
    }

    @GetMapping("/position/account/{accountId}")
    public ResponseEntity<List<ETFInvestmentDTO>> getPositionsByAccount(@CurrentUserId Long userId, @PathVariable Long accountId) {
        return ResponseEntity.ok(this.etfInvestmentService.getPositionsByAccountId(userId, accountId));
    }

    @PostMapping
    public ResponseEntity<ETFInvestmentDTO> createInvestment(@CurrentUserId Long userId, @RequestBody ETFInvestmentDTO investmentDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.etfInvestmentService.createInvestment(investmentDTO, userId));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserId Long userId, @RequestParam List<Long> ids) {
        this.etfInvestmentService.deleteInvestmentById(userId, ids);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/csv")
    public void getCSV(@CurrentUserId Long userId, HttpServletResponse servletResponse) throws IOException {
        this.etfInvestmentService.getCSV(userId, CsvResponseUtil.prepare(servletResponse, "etf_investments.csv"));
    }

    @PutMapping
    public ResponseEntity<ETFInvestmentDTO> updateInvestment(@CurrentUserId Long userId, @RequestBody ETFInvestmentDTO investmentDTO) {
        return ResponseEntity.ok(this.etfInvestmentService.updateInvestment(investmentDTO, userId));
    }

    @PostMapping("/process/csv")
    public ResponseEntity<Void> processCSV(@CurrentUserId Long userId, @RequestParam("file") MultipartFile file) throws IOException {
        this.etfInvestmentService.processCSV(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
