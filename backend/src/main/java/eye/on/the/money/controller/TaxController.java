package eye.on.the.money.controller;

import eye.on.the.money.dto.in.RSUDTO;
import eye.on.the.money.dto.in.TaxAmountDTO;
import eye.on.the.money.dto.out.RSUTaxEventReportDTO;
import eye.on.the.money.dto.out.RSUVestTaxEventReportDTO;
import eye.on.the.money.dto.out.TaxBreakdownDTO;
import eye.on.the.money.dto.out.TaxReportDTO;
import eye.on.the.money.dto.out.TaxableEventReportDTO;
import eye.on.the.money.security.CurrentUserId;
import eye.on.the.money.service.financial.TaxableEventService;
import eye.on.the.money.service.shared.TaxService;
import eye.on.the.money.service.stock.RSUTaxService;
import eye.on.the.money.service.stock.RSUVestTaxService;
import eye.on.the.money.util.CsvResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@Validated
@RequiredArgsConstructor
public class TaxController {

    private final TaxService taxService;
    private final TaxableEventService taxableEventService;
    private final RSUTaxService rsuTaxService;
    private final RSUVestTaxService rsuVestTaxService;

    @GetMapping("/transaction")
    public ResponseEntity<TaxableEventReportDTO> getTaxableEvents(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.taxableEventService.getTaxableEvents(userId));
    }

    @GetMapping("/transaction/csv")
    public void getTaxableEventsCSV(@CurrentUserId Long userId, HttpServletResponse servletResponse)
            throws IOException {
        this.taxableEventService.getCSV(userId, CsvResponseUtil.prepare(servletResponse, "taxable-events.csv"));
    }

    @PutMapping("/transaction/paid")
    public ResponseEntity<Void> setTaxPaid(@CurrentUserId Long userId, @RequestParam List<Long> ids,
                                           @RequestParam boolean paid) {
        this.taxableEventService.setTaxPaid(userId, ids, paid);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stock")
    public ResponseEntity<RSUTaxEventReportDTO> getRSUTaxEvents(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.rsuTaxService.getRSUTaxEvents(userId));
    }

    @GetMapping("/stock/csv")
    public void getRSUTaxEventsCSV(@CurrentUserId Long userId, HttpServletResponse servletResponse)
            throws IOException {
        this.rsuTaxService.getCSV(userId, CsvResponseUtil.prepare(servletResponse, "rsu-transactions.csv"));
    }

    @PutMapping("/stock/paid")
    public ResponseEntity<Void> setRSUTaxPaid(@CurrentUserId Long userId, @RequestParam List<Long> ids,
                                              @RequestParam boolean paid) {
        this.rsuTaxService.setTaxPaid(userId, ids, paid);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/equity")
    public ResponseEntity<RSUVestTaxEventReportDTO> getRSUVestTaxEvents(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.rsuVestTaxService.getVestTaxEvents(userId));
    }

    @GetMapping("/equity/csv")
    public void getRSUVestTaxEventsCSV(@CurrentUserId Long userId, HttpServletResponse servletResponse)
            throws IOException {
        this.rsuVestTaxService.getCSV(userId, CsvResponseUtil.prepare(servletResponse, "rsu-vests.csv"));
    }

    @PutMapping("/equity/paid")
    public ResponseEntity<Void> setRSUVestTaxPaid(@CurrentUserId Long userId, @RequestParam List<Long> ids,
                                                  @RequestParam boolean paid) {
        this.rsuVestTaxService.setTaxPaid(userId, ids, paid);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/equity/recalculate")
    public ResponseEntity<Void> recalculateRSUVestTax(@CurrentUserId Long userId, @RequestParam List<Long> ids) {
        this.rsuVestTaxService.recalculate(userId, ids);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/amount")
    public ResponseEntity<TaxBreakdownDTO> calculateTax(@RequestBody @Valid TaxAmountDTO amountDTO) {
        return ResponseEntity.ok(this.taxService.calculateTax(amountDTO.getAmount()));
    }

    @PostMapping("/rsu")
    public ResponseEntity<TaxReportDTO> calculateTaxForRSUs(@RequestBody List<@Valid RSUDTO> rsus) {
        return ResponseEntity.ok(this.taxService.calculateTaxForRSUs(rsus));
    }

    @PostMapping("/rsu/csv")
    public void getCSV(@RequestBody List<@Valid RSUDTO> rsus, HttpServletResponse servletResponse)
            throws IOException {
        this.taxService.getCSV(rsus, CsvResponseUtil.prepare(servletResponse, "rsu-tax.csv"));
    }
}
