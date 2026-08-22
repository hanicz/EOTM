package eye.on.the.money.controller;

import eye.on.the.money.dto.in.RSUDTO;
import eye.on.the.money.dto.in.TaxAmountDTO;
import eye.on.the.money.dto.out.TaxBreakdownDTO;
import eye.on.the.money.dto.out.TaxReportDTO;
import eye.on.the.money.dto.out.TaxableEventReportDTO;
import eye.on.the.money.security.CurrentUserEmail;
import eye.on.the.money.service.financial.TaxableEventService;
import eye.on.the.money.service.shared.TaxService;
import eye.on.the.money.util.CsvResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping("/transaction")
    public ResponseEntity<TaxableEventReportDTO> getTaxableEvents(@CurrentUserEmail String userEmail) {
        log.trace("Enter");
        return ResponseEntity.ok(this.taxableEventService.getTaxableEvents(userEmail));
    }

    @GetMapping("/transaction/csv")
    public void getTaxableEventsCSV(@CurrentUserEmail String userEmail, HttpServletResponse servletResponse)
            throws IOException {
        log.trace("Enter");
        this.taxableEventService.getCSV(userEmail, CsvResponseUtil.prepare(servletResponse, "taxable-events.csv"));
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

    /**
     * A POST rather than the GET the other modules use, because the report is computed from the supplied
     * rows rather than read back from the database.
     */
    @PostMapping("/rsu/csv")
    public void getCSV(@RequestBody List<@Valid RSUDTO> rsus, HttpServletResponse servletResponse)
            throws IOException {
        log.trace("Enter");
        this.taxService.getCSV(rsus, CsvResponseUtil.prepare(servletResponse, "rsu-tax.csv"));
    }
}
