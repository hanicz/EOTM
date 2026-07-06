package eye.on.the.money.controller;

import eye.on.the.money.dto.out.DashboardRatesDTO;
import eye.on.the.money.service.shared.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/dashboard")
@Slf4j
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("rates")
    public ResponseEntity<DashboardRatesDTO> getConversionRates(@RequestParam(required = false) List<String> currencies) {
        log.trace("Enter");
        return ResponseEntity.ok(this.dashboardService.getConversionRates(currencies == null ? List.of() : currencies));
    }
}
