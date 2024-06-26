package eye.on.the.money.controller;

import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.service.stock.InvestmentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    public ResponseEntity<List<InvestmentDTO>> getAllInvestments(@AuthenticationPrincipal UserDetails user) {
        return new ResponseEntity<>(this.investmentService.getInvestments(user.getUsername()), HttpStatus.OK);
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<InvestmentDTO>> getInvestmentsByAccount(@AuthenticationPrincipal UserDetails user, @PathVariable Long accountId) {
        return new ResponseEntity<>(this.investmentService.getInvestmentsByAccountId(user.getUsername(), accountId), HttpStatus.OK);
    }

    @GetMapping("/holding")
    public ResponseEntity<List<InvestmentDTO>> getHoldings(@AuthenticationPrincipal UserDetails user) {
        return new ResponseEntity<>(this.investmentService.getCurrentHoldings(user.getUsername()), HttpStatus.OK);
    }

    @GetMapping("/holding/account/{accountId}")
    public ResponseEntity<List<InvestmentDTO>> getHoldingsByAccount(@AuthenticationPrincipal UserDetails user, @PathVariable Long accountId) {
        return new ResponseEntity<>(this.investmentService.getHoldingsByAccountId(user.getUsername(), accountId), HttpStatus.OK);
    }

    @GetMapping("/position")
    public ResponseEntity<List<InvestmentDTO>> getPositions(@AuthenticationPrincipal UserDetails user) {
        return new ResponseEntity<>(this.investmentService.getAllPositions(user.getUsername()), HttpStatus.OK);
    }

    @GetMapping("/position/account/{accountId}")
    public ResponseEntity<List<InvestmentDTO>> getPositionsByAccount(@AuthenticationPrincipal UserDetails user, @PathVariable Long accountId) {
        return new ResponseEntity<>(this.investmentService.getPositionsByAccountId(user.getUsername(), accountId), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<InvestmentDTO> createInvestment(@AuthenticationPrincipal UserDetails user, @RequestBody InvestmentDTO investmentDTO) {
        return new ResponseEntity<>(this.investmentService.createInvestment(investmentDTO, user.getUsername()), HttpStatus.CREATED);
    }

    @DeleteMapping()
    public ResponseEntity<HttpStatus> deleteByIds(@AuthenticationPrincipal UserDetails user, @RequestParam String ids) {
        this.investmentService.deleteInvestmentById(user.getUsername(), ids);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/csv")
    public void getCSV(@AuthenticationPrincipal UserDetails user, HttpServletResponse servletResponse) throws IOException {
        servletResponse.setContentType("text/csv");
        servletResponse.addHeader("Content-Disposition", "attachment; filename=\"investments.csv\"");
        this.investmentService.getCSV(user.getUsername(), servletResponse.getWriter());
    }

    @PutMapping
    public ResponseEntity<InvestmentDTO> updateInvestment(@AuthenticationPrincipal UserDetails user, @RequestBody InvestmentDTO investmentDTO) {
        return new ResponseEntity<>(this.investmentService.updateInvestment(investmentDTO, user.getUsername()), HttpStatus.CREATED);
    }

    @PostMapping("/process/csv")
    public ResponseEntity<HttpStatus> processCSV(@AuthenticationPrincipal UserDetails user, @RequestParam("file") MultipartFile file) throws IOException {
        this.investmentService.processCSV(user.getUsername(), file);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
