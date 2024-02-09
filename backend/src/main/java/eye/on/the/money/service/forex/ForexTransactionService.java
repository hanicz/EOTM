package eye.on.the.money.service.forex;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.out.ForexTransactionDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.forex.ForexTransaction;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.forex.ForexTransactionRepository;
import eye.on.the.money.service.user.UserServiceImpl;
import eye.on.the.money.service.api.EODAPIService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ForexTransactionService {

    private final CurrencyRepository currencyRepository;
    private final ForexTransactionRepository forexTransactionRepository;
    private final UserServiceImpl userService;
    private final ModelMapper modelMapper;
    private final EODAPIService eodAPIService;

    public List<ForexTransactionDTO> getForexTransactionsByUserId(String userEmail) {
        return this.forexTransactionRepository.findByUserEmailOrderByTransactionDate(userEmail).stream().map(this::convertToForexTransactionDTO).collect(Collectors.toList());
    }

    private ForexTransactionDTO convertToForexTransactionDTO(ForexTransaction forexTransaction) {
        return this.modelMapper.map(forexTransaction, ForexTransactionDTO.class);
    }

    @Transactional
    public void deleteForexTransactionById(String userEmail, String ids) {
        List<Long> idList = Stream.of(ids.split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        this.forexTransactionRepository.deleteByUserEmailAndIdIn(userEmail, idList);
    }

    @Transactional
    public ForexTransactionDTO createForexTransaction(ForexTransactionDTO forexTransactionDTO, String userEmail) {
        Currency toCurrency = this.currencyRepository.findById(forexTransactionDTO.getToCurrencyId()).orElseThrow(NoSuchElementException::new);
        Currency fromCurrency = this.currencyRepository.findById(forexTransactionDTO.getFromCurrencyId()).orElseThrow(NoSuchElementException::new);
        User user = this.userService.loadUserByEmail(userEmail);

        ForexTransaction forexTransaction = ForexTransaction.builder()
                .buySell(forexTransactionDTO.getBuySell())
                .transactionDate(forexTransactionDTO.getTransactionDate())
                .toCurrency(toCurrency)
                .fromCurrency(fromCurrency)
                .fromAmount(forexTransactionDTO.getFromAmount())
                .toAmount(forexTransactionDTO.getToAmount())
                .changeRate(forexTransactionDTO.getBuySell().equals("B") ? forexTransactionDTO.getFromAmount() / forexTransactionDTO.getToAmount() : forexTransactionDTO.getToAmount() / forexTransactionDTO.getFromAmount())
                .user(user)
                .build();

        forexTransaction = this.forexTransactionRepository.save(forexTransaction);
        return this.convertToForexTransactionDTO(forexTransaction);
    }

    @Transactional
    public ForexTransactionDTO updateForexTransaction(ForexTransactionDTO forexTransactionDTO, String userEmail) {
        Currency toCurrency = this.currencyRepository.findById(forexTransactionDTO.getToCurrencyId()).orElseThrow(NoSuchElementException::new);
        Currency fromCurrency = this.currencyRepository.findById(forexTransactionDTO.getFromCurrencyId()).orElseThrow(NoSuchElementException::new);
        ForexTransaction forexTransaction = this.forexTransactionRepository.findByIdAndUserEmail(forexTransactionDTO.getForexTransactionId(), userEmail).orElseThrow(NoSuchElementException::new);

        forexTransaction.setBuySell(forexTransactionDTO.getBuySell());
        forexTransaction.setTransactionDate(forexTransactionDTO.getTransactionDate());
        forexTransaction.setFromAmount(forexTransactionDTO.getFromAmount());
        forexTransaction.setToAmount(forexTransactionDTO.getToAmount());
        forexTransaction.setToCurrency(toCurrency);
        forexTransaction.setFromCurrency(fromCurrency);
        forexTransaction.setChangeRate(forexTransactionDTO.getBuySell().equals("B") ? forexTransactionDTO.getFromAmount() / forexTransactionDTO.getToAmount() : forexTransactionDTO.getToAmount() / forexTransactionDTO.getFromAmount());

        return this.convertToForexTransactionDTO(forexTransaction);
    }

    public List<ForexTransactionDTO> getAllForexHoldings(String userEmail) {
        Map<String, ForexTransactionDTO> forexTransactionMap = this.getCalculated(userEmail);
        List<ForexTransactionDTO> forexTransactions = new ArrayList<>(forexTransactionMap.values());
        String joinedList = forexTransactions.stream().map(f -> (f.getToCurrencyId() + f.getFromCurrencyId() + ".FOREX")).collect(Collectors.joining(","));

        JsonNode responseBody = this.eodAPIService.getLiveValue(joinedList, "/real-time/forex/?api_token={0}&fmt=json&s={1}");
        for (JsonNode forex : responseBody) {
            Optional<ForexTransactionDTO> forexTransactionDTO = forexTransactions.stream().filter
                    (f -> (f.getToCurrencyId() + f.getFromCurrencyId() + ".FOREX").equals(forex.findValue("code").textValue())).findFirst();
            if (forexTransactionDTO.isEmpty()) continue;
            forexTransactionDTO.get().setLiveValue(forex.findValue("close").doubleValue() * forexTransactionDTO.get().getToAmount());
            forexTransactionDTO.get().setLiveChangeRate(forex.findValue("close").doubleValue());
            forexTransactionDTO.get().setValueDiff(forexTransactionDTO.get().getLiveValue() - forexTransactionDTO.get().getFromAmount());
        }

        return forexTransactions;
    }

    private Map<String, ForexTransactionDTO> getCalculated(String userEmail) {
        List<ForexTransactionDTO> forexTransactions = this.forexTransactionRepository.findByUserEmailOrderByTransactionDate(userEmail).stream().map(this::convertToForexTransactionDTO).collect(Collectors.toList());
        Map<String, ForexTransactionDTO> forexTransactionMap = new HashMap<>();
        for (ForexTransactionDTO ft : forexTransactions) {
            String symbol = ft.getFromCurrencyId() + ft.getToCurrencyId();
            forexTransactionMap.compute(symbol, (key, value) -> (value == null) ? ft : value.mergeTransactions(ft));
        }
        return forexTransactionMap;
    }
}
