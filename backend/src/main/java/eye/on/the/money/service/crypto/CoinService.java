package eye.on.the.money.service.crypto;

import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.repository.crypto.CoinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CoinService {

    private final CoinRepository coinRepository;

    public List<Coin> getAllCoins() {
        return this.coinRepository.findAllByOrderByNameAsc();
    }

    public Coin getCoinBySymbol(String symbol) {
        return this.coinRepository.findBySymbol(symbol).orElseThrow(NoSuchElementException::new);
    }
}
