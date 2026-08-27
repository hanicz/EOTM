package eye.on.the.money.controller;

import eye.on.the.money.dto.in.RSUDTO;
import eye.on.the.money.dto.in.TaxAmountDTO;
import eye.on.the.money.dto.out.RSUTaxEventReportDTO;
import eye.on.the.money.dto.out.TaxBreakdownDTO;
import eye.on.the.money.dto.out.TaxReportDTO;
import eye.on.the.money.dto.out.TaxableEventReportDTO;
import eye.on.the.money.security.CurrentUserId;
import eye.on.the.money.service.financial.TaxableEventService;
import eye.on.the.money.service.shared.TaxService;
import eye.on.the.money.service.stock.RSUTaxService;
import eye.on.the.money.util.CsvResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/v1/tax")
@Slf4j
@Validated
@RequiredArgsConstructor
public class TaxController {

    private final TaxService taxService;
    private final TaxableEventService taxableEventService;
    private final RSUTaxService rsuTaxService;

    @GetMapping("/transaction")
    public ResponseEntity<TaxableEventReportDTO> getTaxableEvents(@CurrentUserId Long userId) {
        log.trace("Enter");
        return ResponseEntity.ok(this.taxableEventService.getTaxableEvents(userId));
    }

    @GetMapping("/transaction/csv")
    public void getTaxableEventsCSV(@CurrentUserId Long userId, HttpServletResponse servletResponse)
            throws IOException {
        log.trace("Enter");
        this.taxableEventService.getCSV(userId, CsvResponseUtil.prepare(servletResponse, "taxable-events.csv"));
    }

    @PutMapping("/transaction/paid")
    public ResponseEntity<Void> setTaxPaid(@CurrentUserId Long userId, @RequestParam List<Long> ids,
                                           @RequestParam boolean paid) {
        log.trace("Enter");
        this.taxableEventService.setTaxPaid(userId, ids, paid);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stock")
    public ResponseEntity<RSUTaxEventReportDTO> getRSUTaxEvents(@CurrentUserId Long userId) {
        log.trace("Enter");
        return ResponseEntity.ok(this.rsuTaxService.getRSUTaxEvents(userId));
    }

    @GetMapping("/stock/csv")
    public void getRSUTaxEventsCSV(@CurrentUserId Long userId, HttpServletResponse servletResponse)
            throws IOException {
        log.trace("Enter");
        this.rsuTaxService.getCSV(userId, CsvResponseUtil.prepare(servletResponse, "rsu-transactions.csv"));
    }

    @PutMapping("/stock/paid")
    public ResponseEntity<Void> setRSUTaxPaid(@CurrentUserId Long userId, @RequestParam List<Long> ids,
                                              @RequestParam boolean paid) {
        log.trace("Enter");
        this.rsuTaxService.setTaxPaid(userId, ids, paid);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/amount")
    public ResponseEntity<TaxBreakdownDTO> calculateTax(@RequestBody @Valid TaxAmountDTO amountDTO) {
        log.trace("Enter");
        return ResponseEntity.ok(this.taxService.calculateTax(amountDTO.getAmount()));
    }

    @PostMapping("/rsu")
    public ResponseEntity<TaxReportDTO> calculateTaxForRSUs(@RequestBody List<@Valid RSUDTO> rsus) {
        log.trace("Enter");
        return ResponseEntity.ok(this.taxService.calculateTaxForRSUs(rsus));
    }

    @PostMapping("/rsu/csv")
    public void getCSV(@RequestBody List<@Valid RSUDTO> rsus, HttpServletResponse servletResponse)
            throws IOException {
        log.trace("Enter");
        this.taxService.getCSV(rsus, CsvResponseUtil.prepare(servletResponse, "rsu-tax.csv"));
    }
}
