package eye.on.the.money.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.out.CryptoWatchDTO;
import eye.on.the.money.dto.out.ForexWatchDTO;
import eye.on.the.money.dto.out.StockWatchDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.watchlist.CryptoWatch;
import eye.on.the.money.model.watchlist.ForexWatch;
import eye.on.the.money.model.watchlist.TickerWatch;
import eye.on.the.money.repository.crypto.CoinRepository;
import eye.on.the.money.repository.stock.StockRepository;
import eye.on.the.money.repository.watchlist.CryptoWatchRepository;
import eye.on.the.money.repository.watchlist.ForexWatchRepository;
import eye.on.the.money.repository.watchlist.StockWatchRepository;
import eye.on.the.money.service.WatchlistService;
import eye.on.the.money.service.api.CryptoAPIService;
import eye.on.the.money.service.api.EODAPIService;
import eye.on.the.money.service.stock.StockService;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
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
    private EODAPIService eodAPIService;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private StockService stockService;

    @Override
    public List<CryptoWatchDTO> getCryptoWatchlistByUserId(Long userId, String currency) {
        List<CryptoWatchDTO> cryptoList = this.cryptoWatchRepository.findByUser_IdOrderByCoin_Symbol(userId).stream()
                .map(this::convertToCryptoWatchDTO).collect(Collectors.toList());
        this.cryptoAPIService.getLiveValueForWatchList(cryptoList, currency);
        return cryptoList;
    }

    @Override
    public List<StockWatchDTO> getStockWatchlistByUserId(Long userId) {
        List<StockWatchDTO> stockList = this.stockWatchRepository.findByUser_IdOrderByStockShortName(userId).stream()
                .map(this::convertToStockWatchDTO).collect(Collectors.toList());
        String joinedList = stockList.stream().map(s -> (s.getStockShortName() + "." + s.getStockExchange())).collect(Collectors.joining(","));
        JsonNode responseBody = this.eodAPIService.getLiveValue(joinedList, "/real-time/stock/?api_token={0}&fmt=json&s={1}");

        for (JsonNode stock : responseBody) {
            Optional<StockWatchDTO> stockWatchDTO = stockList.stream().filter
                    (s -> (s.getStockShortName() + "." + s.getStockExchange()).equals(stock.findValue("code").textValue())).findFirst();
            if (stockWatchDTO.isEmpty()) continue;

            stockWatchDTO.get().setLiveValue(stock.findValue("close").doubleValue());
            stockWatchDTO.get().setChange(stock.findValue("change").doubleValue());
            stockWatchDTO.get().setPChange(stock.findValue("change_p").doubleValue());
            stockWatchDTO.get().setCurrencyId("USD");
        }

        return stockList;
    }

    @Override
    public List<ForexWatchDTO> getForexWatchlistByUserId(Long userId) {
        List<ForexWatchDTO> forexList = this.forexWatchRepository.findByUser_Id(userId).stream()
                .map(this::convertToForexDTO).collect(Collectors.toList());
        String joinedList = forexList.stream().map(f -> (f.getFromCurrencyId() + f.getToCurrencyId() + ".FOREX")).collect(Collectors.joining(","));
        JsonNode responseBody = this.eodAPIService.getLiveValue(joinedList, "/real-time/forex/?api_token={0}&fmt=json&s={1}");

        for (JsonNode forex : responseBody) {
            Optional<ForexWatchDTO> forexWatchDTO = forexList.stream().filter
                    (f -> (f.getFromCurrencyId() + f.getToCurrencyId() + ".FOREX").equals(forex.findValue("code").textValue())).findFirst();
            if (forexWatchDTO.isEmpty()) continue;
            forexWatchDTO.get().setLiveValue(forex.findValue("close").doubleValue());
            forexWatchDTO.get().setChange(forex.findValue("change").doubleValue() * -1);
            forexWatchDTO.get().setPChange(forex.findValue("change_p").doubleValue() * -1);
        }

        return forexList;
    }

    @Transactional
    @Override
    public void deleteStockWatchById(Long userid, Long id) {
        this.stockWatchRepository.deleteByIdAndUser_Id(id, userid);
    }

    @Transactional
    @Override
    public void deleteCryptoWatchById(Long userid, Long id) {
        this.cryptoWatchRepository.deleteByIdAndUser_Id(id, userid);
    }

    @Transactional
    @Override
    public void deleteForexWatchById(Long userid, Long id) {
        this.forexWatchRepository.deleteByIdAndUser_Id(id, userid);
    }

    @Transactional
    @Override
    public StockWatchDTO createNewStockWatch(User user, Stock wStock) {
        Stock stock = this.stockService.getOrCreateStock(wStock.getShortName(), wStock.getExchange(), wStock.getName());

        TickerWatch tickerWatch = TickerWatch.builder().stock(stock).user(user).build();
        this.stockWatchRepository.save(tickerWatch);
        return this.convertToStockWatchDTO(tickerWatch);
    }

    @Transactional
    @Override
    public CryptoWatchDTO createNewCryptoWatch(User user, String coinId) {
        Coin coin = this.coinRepository.findById(coinId).orElseThrow(NoSuchElementException::new);

        CryptoWatch cryptoWatch = CryptoWatch.builder().coin(coin).user(user).build();
        this.cryptoWatchRepository.save(cryptoWatch);
        return this.convertToCryptoWatchDTO(cryptoWatch);
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
