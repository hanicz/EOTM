package eye.on.the.money.controller;

import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.service.crypto.CoinService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("coin")
@Slf4j
public class CoinController {

    private final CoinService coinService;

    @Autowired
    public CoinController(CoinService coinService) {
        this.coinService = coinService;
    }

    @GetMapping()
    public ResponseEntity<List<Coin>> getAllCoins() {
        log.trace("Enter");
        return new ResponseEntity<>(this.coinService.getAllCoins(), HttpStatus.OK);
    }
}
