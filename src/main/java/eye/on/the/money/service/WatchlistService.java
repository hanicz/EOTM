package eye.on.the.money.service;

import eye.on.the.money.dto.out.CryptoWatchDTO;
import eye.on.the.money.dto.out.ForexWatchDTO;
import eye.on.the.money.dto.out.StockWatchDTO;

import java.util.List;

public interface WatchlistService {
    public List<CryptoWatchDTO> getCryptoWatchlistByUserId(Long userId, String currency);
    public List<ForexWatchDTO> getForexWatchlistByUserId(Long userId);
    public List<StockWatchDTO> getStockWatchlistByUserId(Long userId);
}
