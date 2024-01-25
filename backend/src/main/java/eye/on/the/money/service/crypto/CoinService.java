package eye.on.the.money.service.crypto;

import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.repository.crypto.CoinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CoinService {

    private final CoinRepository coinRepository;

    public List<Coin> getAllCoins() {
        return this.coinRepository.findAllByOrderByNameAsc();
    }
}
