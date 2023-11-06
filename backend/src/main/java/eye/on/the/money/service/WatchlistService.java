package eye.on.the.money.service;

import eye.on.the.money.dto.out.CryptoWatchDTO;
import eye.on.the.money.dto.out.ForexWatchDTO;
import eye.on.the.money.dto.out.StockWatchDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Stock;

import java.util.List;

public interface WatchlistService {
    public List<CryptoWatchDTO> getCryptoWatchlistByUserId(Long userId, String currency);

    public List<ForexWatchDTO> getForexWatchlistByUserId(Long userId);

    public List<StockWatchDTO> getStockWatchlistByUserId(Long userId);

    public void deleteStockWatchById(Long userid, Long id);

    public void deleteCryptoWatchById(Long userid, Long id);

    public void deleteForexWatchById(Long userid, Long id);

    public StockWatchDTO createNewStockWatch(User user, Stock wStock);

    public CryptoWatchDTO createNewCryptoWatch(User user, String coinId);
}
