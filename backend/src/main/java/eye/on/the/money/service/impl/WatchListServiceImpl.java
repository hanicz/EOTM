package eye.on.the.money.service.impl;

import eye.on.the.money.dto.out.CryptoWatchDTO;
import eye.on.the.money.dto.out.ForexWatchDTO;
import eye.on.the.money.dto.out.StockWatchDTO;
import eye.on.the.money.model.watchlist.CryptoWatch;
import eye.on.the.money.model.watchlist.ForexWatch;
import eye.on.the.money.model.watchlist.TickerWatch;
import eye.on.the.money.repository.CryptoWatchRepository;
import eye.on.the.money.repository.ForexWatchRepository;
import eye.on.the.money.repository.StockWatchRepository;
import eye.on.the.money.service.WatchlistService;
import eye.on.the.money.service.api.CryptoAPIService;
import eye.on.the.money.service.api.CurrencyConverter;
import eye.on.the.money.service.api.StockAPIService;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WatchListServiceImpl implements WatchlistService {

    @Autowired
    private CryptoWatchRepository cryptoWatchRepository;

    @Autowired
    private StockWatchRepository stockWatchRepository;

    @Autowired
    private ForexWatchRepository forexWatchRepository;

    @Autowired
    private CryptoAPIService cryptoAPIService;

    @Autowired
    private StockAPIService stockAPIService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private CurrencyConverter currencyConverter;

    @Override
    public List<CryptoWatchDTO> getCryptoWatchlistByUserId(Long userId, String currency) {
        List<CryptoWatchDTO> cryptoList = this.cryptoWatchRepository.findByUser_IdOrderByCoin_Symbol(userId).stream().map(this::convertToCryptoWatchDTO).collect(Collectors.toList());
        this.cryptoAPIService.getLiveValueForWatchList(cryptoList, currency);
        return cryptoList;
    }

    @Override
    public List<StockWatchDTO> getStockWatchlistByUserId(Long userId) {
        List<StockWatchDTO> stockList = this.stockWatchRepository.findByUser_IdOrderByStockShortName(userId).stream().map(this::convertToStockWatchDTO).collect(Collectors.toList());
        this.stockAPIService.getStockWatchList(stockList);
        return stockList;
    }

    @Override
    public List<ForexWatchDTO> getForexWatchlistByUserId(Long userId) {
        List<ForexWatchDTO> forexList = this.forexWatchRepository.findByUser_Id(userId).stream().map(this::convertToForexDTO).collect(Collectors.toList());
        this.currencyConverter.changeForexWatchList(forexList);
        return forexList;
    }

    private CryptoWatchDTO convertToCryptoWatchDTO(CryptoWatch cryptoWatch) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(cryptoWatch, CryptoWatchDTO.class);
    }

    private StockWatchDTO convertToStockWatchDTO(TickerWatch tickerWatch) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(tickerWatch, StockWatchDTO.class);
    }

    private ForexWatchDTO convertToForexDTO(ForexWatch forexWatch) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(forexWatch, ForexWatchDTO.class);
    }
}
