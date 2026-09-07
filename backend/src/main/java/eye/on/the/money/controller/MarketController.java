package eye.on.the.money.controller;

import eye.on.the.money.dto.out.MarketExchangeDTO;
import eye.on.the.money.service.market.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;

    @GetMapping()
    public ResponseEntity<List<MarketExchangeDTO>> getExchanges() {
        return ResponseEntity.ok(this.marketService.getExchanges());
    }
}
