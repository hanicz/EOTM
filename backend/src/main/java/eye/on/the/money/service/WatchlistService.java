package eye.on.the.money.service;

import eye.on.the.money.dto.out.CryptoWatchDTO;
import eye.on.the.money.dto.out.ForexWatchDTO;
import eye.on.the.money.dto.out.StockWatchDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Stock;

import java.util.List;

public interface WatchlistService {
    List<CryptoWatchDTO> getCryptoWatchlistByUserId(Long userId, String currency);
    List<ForexWatchDTO> getForexWatchlistByUserId(Long userId);
    List<StockWatchDTO> getStockWatchlistByUserId(Long userId);
    void deleteStockWatchById(Long userid, Long id);
    void deleteCryptoWatchById(Long userid, Long id);
    void deleteForexWatchById(Long userid, Long id);
    StockWatchDTO createNewStockWatch(User user, Stock wStock);
    CryptoWatchDTO createNewCryptoWatch(User user, String coinId);
}
