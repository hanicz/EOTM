package eye.on.the.money.controller;

import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.service.stock.InvestmentService;
import eye.on.the.money.util.CsvResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import eye.on.the.money.security.CurrentUserEmail;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/v1/investment")
@Slf4j
@RequiredArgsConstructor
public class InvestmentController {

    private final InvestmentService investmentService;

    @GetMapping()
    public ResponseEntity<List<InvestmentDTO>> getAllInvestments(@CurrentUserEmail String userEmail) {
        return ResponseEntity.ok(this.investmentService.getInvestments(userEmail));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<InvestmentDTO>> getInvestmentsByAccount(@CurrentUserEmail String userEmail, @PathVariable Long accountId) {
        return ResponseEntity.ok(this.investmentService.getInvestmentsByAccountId(userEmail, accountId));
    }

    @GetMapping("/holding")
    public ResponseEntity<List<InvestmentDTO>> getHoldings(@CurrentUserEmail String userEmail) {
        return ResponseEntity.ok(this.investmentService.getCurrentHoldings(userEmail));
    }

    @GetMapping("/holding/account/{accountId}")
    public ResponseEntity<List<InvestmentDTO>> getHoldingsByAccount(@CurrentUserEmail String userEmail, @PathVariable Long accountId) {
        return ResponseEntity.ok(this.investmentService.getHoldingsByAccountId(userEmail, accountId));
    }

    @GetMapping("/position")
    public ResponseEntity<List<InvestmentDTO>> getPositions(@CurrentUserEmail String userEmail) {
        return ResponseEntity.ok(this.investmentService.getAllPositions(userEmail));
    }

    @GetMapping("/position/account/{accountId}")
    public ResponseEntity<List<InvestmentDTO>> getPositionsByAccount(@CurrentUserEmail String userEmail, @PathVariable Long accountId) {
        return ResponseEntity.ok(this.investmentService.getPositionsByAccountId(userEmail, accountId));
    }

    @PostMapping
    public ResponseEntity<InvestmentDTO> createInvestment(@CurrentUserEmail String userEmail, @RequestBody InvestmentDTO investmentDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.investmentService.createInvestment(investmentDTO, userEmail));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserEmail String userEmail, @RequestParam List<Long> ids) {
        this.investmentService.deleteInvestmentById(userEmail, ids);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/csv")
    public void getCSV(@CurrentUserEmail String userEmail, HttpServletResponse servletResponse) throws IOException {
        this.investmentService.getCSV(userEmail, CsvResponseUtil.prepare(servletResponse, "investments.csv"));
    }

    @PutMapping
    public ResponseEntity<InvestmentDTO> updateInvestment(@CurrentUserEmail String userEmail, @RequestBody InvestmentDTO investmentDTO) {
        return ResponseEntity.ok(this.investmentService.updateInvestment(investmentDTO, userEmail));
    }

    @PostMapping("/process/csv")
    public ResponseEntity<Void> processCSV(@CurrentUserEmail String userEmail, @RequestParam("file") MultipartFile file) throws IOException {
        this.investmentService.processCSV(userEmail, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
