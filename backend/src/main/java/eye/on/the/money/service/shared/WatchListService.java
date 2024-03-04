package eye.on.the.money.service.shared;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.out.CryptoWatchDTO;
import eye.on.the.money.dto.out.ForexWatchDTO;
import eye.on.the.money.dto.out.StockWatchDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.watchlist.CryptoWatch;
import eye.on.the.money.model.watchlist.ForexWatch;
import eye.on.the.money.model.watchlist.TickerWatch;
import eye.on.the.money.repository.crypto.CoinRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.watchlist.CryptoWatchRepository;
import eye.on.the.money.repository.watchlist.ForexWatchRepository;
import eye.on.the.money.repository.watchlist.StockWatchRepository;
import eye.on.the.money.service.user.UserServiceImpl;
import eye.on.the.money.service.api.CryptoAPIService;
import eye.on.the.money.service.api.EODAPIService;
import eye.on.the.money.service.stock.StockService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchListService {

    private final CryptoWatchRepository cryptoWatchRepository;
    private final StockWatchRepository stockWatchRepository;
    private final ForexWatchRepository forexWatchRepository;
    private final CurrencyRepository currencyRepository;
    private final CryptoAPIService cryptoAPIService;
    private final UserServiceImpl userService;
    private final EODAPIService eodAPIService;
    private final CoinRepository coinRepository;
    private final ModelMapper modelMapper;
    private final StockService stockService;

    public List<CryptoWatchDTO> getCryptoWatchlistByUserId(String userEmail, String currency) {
        List<CryptoWatchDTO> cryptoList = this.cryptoWatchRepository.findByUserEmailOrderByCoin_Symbol(userEmail).stream()
                .map(this::convertToCryptoWatchDTO).collect(Collectors.toList());

        String ids = cryptoList.stream().map(CryptoWatchDTO::getCoinId).collect(Collectors.joining(","));
        JsonNode root = this.cryptoAPIService.getLiveValueForCoins(currency, ids);

        cryptoList.forEach(cryptoWatchDTO -> {
            cryptoWatchDTO.setLiveValue(root.path(cryptoWatchDTO.getCoinId()).get(currency.toLowerCase()).doubleValue());
            cryptoWatchDTO.setChange(root.path(cryptoWatchDTO.getCoinId()).get(currency.toLowerCase() + "_24h_change").doubleValue());
        });

        return cryptoList;
    }

    public List<StockWatchDTO> getStockWatchlistByUserId(String userEmail) {
        List<StockWatchDTO> stockList = this.stockWatchRepository.findByUserEmailOrderByStockShortName(userEmail).stream()
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

    public List<ForexWatchDTO> getForexWatchlistByUserId(String userEmail) {
        List<ForexWatchDTO> forexList = this.forexWatchRepository.findByUserEmailOrderByFromCurrencyAscToCurrencyAsc(userEmail).stream()
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
    public void deleteStockWatchById(String userEmail, Long id) {
        this.stockWatchRepository.deleteByIdAndUserEmail(id, userEmail);
    }

    @Transactional
    public void deleteCryptoWatchById(String userEmail, Long id) {
        this.cryptoWatchRepository.deleteByIdAndUserEmail(id, userEmail);
    }

    @Transactional
    public void deleteForexWatchById(String userEmail, Long id) {
        this.forexWatchRepository.deleteByIdAndUserEmail(id, userEmail);
    }

    @Transactional
    public StockWatchDTO createNewStockWatch(String userEmail, Stock wStock) {
        Stock stock = this.stockService.getOrCreateStock(wStock.getShortName(), wStock.getExchange(), wStock.getName());
        User user = this.userService.loadUserByEmail(userEmail);

        TickerWatch tickerWatch = TickerWatch.builder().stock(stock).user(user).build();
        this.stockWatchRepository.save(tickerWatch);
        return this.convertToStockWatchDTO(tickerWatch);
    }

    @Transactional
    public CryptoWatchDTO createNewCryptoWatch(String userEmail, String coinId) {
        Coin coin = this.coinRepository.findById(coinId).orElseThrow(NoSuchElementException::new);
        User user = this.userService.loadUserByEmail(userEmail);

        CryptoWatch cryptoWatch = CryptoWatch.builder().coin(coin).user(user).build();
        this.cryptoWatchRepository.save(cryptoWatch);
        return this.convertToCryptoWatchDTO(cryptoWatch);
    }

    @Transactional
    public ForexWatchDTO createNewForexWatch(String userEmail, String fromCurrencyId, String toCurrencyId) {
        User user = this.userService.loadUserByEmail(userEmail);
        Currency fromCurrency = this.currencyRepository.findById(fromCurrencyId).orElseThrow(NoSuchElementException::new);
        Currency toCurrency = this.currencyRepository.findById(toCurrencyId).orElseThrow(NoSuchElementException::new);

        ForexWatch forexWatch = ForexWatch.builder().fromCurrency(fromCurrency).toCurrency(toCurrency).user(user).build();
        this.forexWatchRepository.save(forexWatch);
        return this.convertToForexDTO(forexWatch);
    }

    private CryptoWatchDTO convertToCryptoWatchDTO(CryptoWatch cryptoWatch) {
        return this.modelMapper.map(cryptoWatch, CryptoWatchDTO.class);
    }

    private StockWatchDTO convertToStockWatchDTO(TickerWatch tickerWatch) {
        return this.modelMapper.map(tickerWatch, StockWatchDTO.class);
    }

    private ForexWatchDTO convertToForexDTO(ForexWatch forexWatch) {
        return this.modelMapper.map(forexWatch, ForexWatchDTO.class);
    }
}
