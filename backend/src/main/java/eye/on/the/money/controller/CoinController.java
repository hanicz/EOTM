package eye.on.the.money.controller;

import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.service.crypto.CoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/coin")
@RequiredArgsConstructor
public class CoinController {

    private final CoinService coinService;

    @GetMapping()
    public ResponseEntity<List<Coin>> getAllCoins() {
        return ResponseEntity.ok(this.coinService.getAllCoins());
    }
}
