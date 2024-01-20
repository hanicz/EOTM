package eye.on.the.money.service;

import eye.on.the.money.dto.out.CryptoWatchDTO;
import eye.on.the.money.dto.out.ForexWatchDTO;
import eye.on.the.money.dto.out.StockWatchDTO;
import eye.on.the.money.model.stock.Stock;

import java.util.List;

public interface WatchlistService {
    public List<CryptoWatchDTO> getCryptoWatchlistByUserId(String userEmail, String currency);

    List<ForexWatchDTO> getForexWatchlistByUserId(String userEmail);

    List<StockWatchDTO> getStockWatchlistByUserId(String userEmail);

    void deleteStockWatchById(String userEmail, Long id);

    void deleteCryptoWatchById(String userEmail, Long id);

    void deleteForexWatchById(String userEmail, Long id);

    StockWatchDTO createNewStockWatch(String userEmail, Stock wStock);

    CryptoWatchDTO createNewCryptoWatch(String userEmail, String coinId);
}
