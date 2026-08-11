package eye.on.the.money.controller;

import eye.on.the.money.dto.in.RSUDTO;
import eye.on.the.money.dto.in.TaxAmountDTO;
import eye.on.the.money.dto.out.TaxBreakdownDTO;
import eye.on.the.money.dto.out.TaxReportDTO;
import eye.on.the.money.service.shared.TaxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/tax")
@Slf4j
@Validated
@RequiredArgsConstructor
public class TaxController {

    private final TaxService taxService;

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
}
