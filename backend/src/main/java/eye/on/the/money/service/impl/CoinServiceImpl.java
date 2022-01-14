package eye.on.the.money.service.impl;

import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.repository.crypto.CoinRepository;
import eye.on.the.money.service.CoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CoinServiceImpl implements CoinService {

    @Autowired
    private CoinRepository coinRepository;

    @Override
    public List<Coin> getAllCoins() {
        return this.coinRepository.findAllByOrderByNameAsc();
    }
}
