package eye.on.the.money.controller;

import eye.on.the.money.dto.out.CurrencyDTO;
import eye.on.the.money.service.forex.CurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/currency")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping
    public ResponseEntity<List<CurrencyDTO>> getCurrencies() {
        return ResponseEntity.ok(this.currencyService.getCurrencies());
    }
}
