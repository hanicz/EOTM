package eye.on.the.money.controller;

import eye.on.the.money.model.User;
import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.service.CoinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("coin")
public class CoinController {

    @Autowired
    private CoinService coinService;

    private static final Logger log = LoggerFactory.getLogger(CoinController.class);

    @GetMapping()
    public ResponseEntity<List<Coin>> getAllCoins(@AuthenticationPrincipal User user) {
        log.trace("getAllCoins");
        return new ResponseEntity<List<Coin>>(this.coinService.getAllCoins(), HttpStatus.OK);
    }
}
