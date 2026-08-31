package eye.on.the.money.service.shared;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.out.CryptoWatchDTO;
import eye.on.the.money.dto.out.ForexWatchDTO;
import eye.on.the.money.dto.out.StockWatchDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.watchlist.CryptoWatch;
import eye.on.the.money.model.watchlist.ForexWatch;
import eye.on.the.money.model.watchlist.TickerWatch;
import eye.on.the.money.model.watchlist.WatchGroup;
import eye.on.the.money.repository.crypto.CoinRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.watchlist.CryptoWatchRepository;
import eye.on.the.money.repository.watchlist.ForexWatchRepository;
import eye.on.the.money.repository.watchlist.StockWatchRepository;
import eye.on.the.money.service.user.UserService;
import eye.on.the.money.service.api.CryptoAPIService;
import eye.on.the.money.service.api.EODAPIService;
import eye.on.the.money.service.stock.StockService;
import eye.on.the.money.service.watchlist.WatchGroupService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eye.on.the.money.util.LiveQuote;
import eye.on.the.money.util.Ticker;

import java.util.List;
import java.util.Map;
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
    private final UserService userService;
    private final EODAPIService eodAPIService;
    private final CoinRepository coinRepository;
    private final ModelMapper modelMapper;
    private final StockService stockService;
    private final WatchGroupService watchGroupService;

    public List<CryptoWatchDTO> getCryptoWatchlistByUserId(Long userId, String currency) {
        List<CryptoWatchDTO> cryptoList = this.cryptoWatchRepository.findByUserIdOrderByCoin_Symbol(userId).stream()
                .map(this::convertToCryptoWatchDTO).collect(Collectors.toList());
        if (cryptoList.isEmpty()) return cryptoList;

        String ids = cryptoList.stream().map(CryptoWatchDTO::getCoinId).collect(Collectors.joining(","));
        JsonNode root = this.cryptoAPIService.getLiveValueForCoins(currency, ids);

        cryptoList.forEach(cryptoWatchDTO -> {
            cryptoWatchDTO.setLiveValue(root.path(cryptoWatchDTO.getCoinId()).get(currency.toLowerCase()).doubleValue());
            cryptoWatchDTO.setChange(root.path(cryptoWatchDTO.getCoinId()).get(currency.toLowerCase() + "_24h_change").doubleValue());
        });

        return cryptoList;
    }

    public List<StockWatchDTO> getStockWatchlistByUserId(Long userId) {
        List<StockWatchDTO> stockList = this.stockWatchRepository.findByUserIdOrderByStockShortName(userId).stream()
                .map(this::convertToStockWatchDTO).collect(Collectors.toList());
        if (stockList.isEmpty()) return stockList;

        String joinedList = stockList.stream().map(s -> Ticker.symbol(s.getStockShortName(), s.getStockExchange())).collect(Collectors.joining(","));
        JsonNode responseBody = this.eodAPIService.getLiveStockValue(joinedList);

        Map<String, String> exchangeCurrencies = this.stockService.getAllExchanges().stream()
                .collect(Collectors.toMap(Exchange::getCode, Exchange::getCurrency, (first, _) -> first));

        stockList.forEach(s -> s.setCurrencyId(exchangeCurrencies.getOrDefault(s.getStockExchange(), "USD")));

        for (JsonNode stock : responseBody) {
            Optional<StockWatchDTO> stockWatchDTO = stockList.stream().filter
                    (s -> Ticker.symbol(s.getStockShortName(), s.getStockExchange()).equals(stock.findValue("code").textValue())).findFirst();
            if (stockWatchDTO.isEmpty()) continue;

            LiveQuote.price(stock).ifPresent(price -> {
                stockWatchDTO.get().setLiveValue(price.value());
                stockWatchDTO.get().setStalePrice(price.stale());
            });
            stockWatchDTO.get().setChange(LiveQuote.numericOrZero(stock, "change"));
            stockWatchDTO.get().setPChange(LiveQuote.numericOrZero(stock, "change_p"));
        }

        return stockList;
    }

    public List<ForexWatchDTO> getForexWatchlistByUserId(Long userId) {
        List<ForexWatchDTO> forexList = this.forexWatchRepository.findByUserIdOrderByFromCurrencyAscToCurrencyAsc(userId).stream()
                .map(this::convertToForexDTO).collect(Collectors.toList());
        if (forexList.isEmpty()) return forexList;

        String joinedList = forexList.stream().map(f -> (f.getFromCurrencyId() + f.getToCurrencyId() + ".FOREX")).collect(Collectors.joining(","));
        JsonNode responseBody = this.eodAPIService.getLiveForexValue(joinedList);

        for (JsonNode forex : responseBody) {
            Optional<ForexWatchDTO> forexWatchDTO = forexList.stream().filter
                    (f -> (f.getFromCurrencyId() + f.getToCurrencyId() + ".FOREX").equals(forex.findValue("code").textValue())).findFirst();
            if (forexWatchDTO.isEmpty()) continue;
            LiveQuote.price(forex).ifPresent(price -> {
                forexWatchDTO.get().setLiveValue(price.value());
                forexWatchDTO.get().setStalePrice(price.stale());
            });
            forexWatchDTO.get().setChange(LiveQuote.numericOrZero(forex, "change") * -1);
            forexWatchDTO.get().setPChange(LiveQuote.numericOrZero(forex, "change_p") * -1);
        }

        return forexList;
    }

    @Transactional
    public void deleteStockWatchById(Long userId, Long id) {
        this.stockWatchRepository.deleteByIdAndUserId(id, userId);
    }

    @Transactional
    public void deleteCryptoWatchById(Long userId, Long id) {
        this.cryptoWatchRepository.deleteByIdAndUserId(id, userId);
    }

    @Transactional
    public void deleteForexWatchById(Long userId, Long id) {
        this.forexWatchRepository.deleteByIdAndUserId(id, userId);
    }

    @Transactional
    public StockWatchDTO createNewStockWatch(Long userId, Stock wStock, Long groupId) {
        Stock stock = this.stockService.getOrCreateStock(wStock.getShortName(), wStock.getExchange(), wStock.getName());
        User user = this.userService.getReference(userId);
        WatchGroup group = (groupId == null) ? null : this.watchGroupService.getGroup(userId, groupId);

        TickerWatch tickerWatch = this.stockWatchRepository.findByUserIdAndStockId(userId, stock.getId())
                .orElseGet(() -> this.stockWatchRepository.save(
                        TickerWatch.builder().stock(stock).user(user).group(group).build()));
        return this.convertToStockWatchDTO(tickerWatch);
    }

    @Transactional
    public StockWatchDTO setStockWatchGroup(Long userId, Long id, Long groupId) {
        TickerWatch tickerWatch = this.stockWatchRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Stock watch not found: " + id));
        tickerWatch.setGroup((groupId == null) ? null : this.watchGroupService.getGroup(userId, groupId));
        return this.convertToStockWatchDTO(this.stockWatchRepository.save(tickerWatch));
    }

    @Transactional
    public CryptoWatchDTO createNewCryptoWatch(Long userId, String coinId) {
        Coin coin = this.coinRepository.findById(coinId).orElseThrow(() -> new NoSuchElementException("Coin not found: " + coinId));
        User user = this.userService.getReference(userId);

        CryptoWatch cryptoWatch = this.cryptoWatchRepository.findByUserIdAndCoinId(userId, coin.getId())
                .orElseGet(() -> this.cryptoWatchRepository.save(
                        CryptoWatch.builder().coin(coin).user(user).build()));
        return this.convertToCryptoWatchDTO(cryptoWatch);
    }

    @Transactional
    public ForexWatchDTO createNewForexWatch(Long userId, String fromCurrencyId, String toCurrencyId) {
        User user = this.userService.getReference(userId);
        Currency fromCurrency = this.currencyRepository.findById(fromCurrencyId).orElseThrow(() -> new NoSuchElementException("Currency not found: " + fromCurrencyId));
        Currency toCurrency = this.currencyRepository.findById(toCurrencyId).orElseThrow(() -> new NoSuchElementException("Currency not found: " + toCurrencyId));

        ForexWatch forexWatch = this.forexWatchRepository
                .findByUserIdAndFromCurrencyIdAndToCurrencyId(userId, fromCurrencyId, toCurrencyId)
                .orElseGet(() -> this.forexWatchRepository.save(
                        ForexWatch.builder().fromCurrency(fromCurrency).toCurrency(toCurrency).user(user).build()));
        return this.convertToForexDTO(forexWatch);
    }

    private CryptoWatchDTO convertToCryptoWatchDTO(CryptoWatch cryptoWatch) {
        return this.modelMapper.map(cryptoWatch, CryptoWatchDTO.class);
    }

    private StockWatchDTO convertToStockWatchDTO(TickerWatch tickerWatch) {
        StockWatchDTO dto = this.modelMapper.map(tickerWatch, StockWatchDTO.class);
        WatchGroup group = tickerWatch.getGroup();
        dto.setGroupId(group == null ? null : group.getId());
        dto.setGroupName(group == null ? null : group.getName());
        return dto;
    }

    private ForexWatchDTO convertToForexDTO(ForexWatch forexWatch) {
        return this.modelMapper.map(forexWatch, ForexWatchDTO.class);
    }
}
