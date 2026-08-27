package eye.on.the.money.controller;

import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.service.stock.InvestmentService;
import eye.on.the.money.service.stock.RSUTaxService;
import eye.on.the.money.util.CsvResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import eye.on.the.money.security.CurrentUserId;
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
    private final RSUTaxService rsuTaxService;

    @GetMapping()
    public ResponseEntity<List<InvestmentDTO>> getAllInvestments(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.investmentService.getInvestments(userId));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<InvestmentDTO>> getInvestmentsByAccount(@CurrentUserId Long userId, @PathVariable Long accountId) {
        return ResponseEntity.ok(this.investmentService.getInvestmentsByAccountId(userId, accountId));
    }

    @GetMapping("/holding")
    public ResponseEntity<List<InvestmentDTO>> getHoldings(@CurrentUserId Long userId,
                                                          @RequestParam(defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(refresh ? this.investmentService.refreshCurrentHoldings(userId)
                : this.investmentService.getCurrentHoldings(userId));
    }

    @GetMapping("/holding/account/{accountId}")
    public ResponseEntity<List<InvestmentDTO>> getHoldingsByAccount(@CurrentUserId Long userId, @PathVariable Long accountId) {
        return ResponseEntity.ok(this.investmentService.getHoldingsByAccountId(userId, accountId));
    }

    @GetMapping("/position")
    public ResponseEntity<List<InvestmentDTO>> getPositions(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.investmentService.getAllPositions(userId));
    }

    @GetMapping("/position/account/{accountId}")
    public ResponseEntity<List<InvestmentDTO>> getPositionsByAccount(@CurrentUserId Long userId, @PathVariable Long accountId) {
        return ResponseEntity.ok(this.investmentService.getPositionsByAccountId(userId, accountId));
    }

    @PostMapping
    public ResponseEntity<InvestmentDTO> createInvestment(@CurrentUserId Long userId, @RequestBody InvestmentDTO investmentDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.investmentService.createInvestment(investmentDTO, userId));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserId Long userId, @RequestParam List<Long> ids) {
        this.investmentService.deleteInvestmentById(userId, ids);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/csv")
    public void getCSV(@CurrentUserId Long userId, HttpServletResponse servletResponse) throws IOException {
        this.investmentService.getCSV(userId, CsvResponseUtil.prepare(servletResponse, "investments.csv"));
    }

    @PutMapping
    public ResponseEntity<InvestmentDTO> updateInvestment(@CurrentUserId Long userId, @RequestBody InvestmentDTO investmentDTO) {
        return ResponseEntity.ok(this.investmentService.updateInvestment(investmentDTO, userId));
    }

    @PutMapping("/rsu")
    public ResponseEntity<Void> setRSU(@CurrentUserId Long userId, @RequestParam List<Long> ids,
                                       @RequestParam boolean rsu) {
        this.rsuTaxService.setRSU(userId, ids, rsu);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/process/csv")
    public ResponseEntity<Void> processCSV(@CurrentUserId Long userId, @RequestParam("file") MultipartFile file) throws IOException {
        this.investmentService.processCSV(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
