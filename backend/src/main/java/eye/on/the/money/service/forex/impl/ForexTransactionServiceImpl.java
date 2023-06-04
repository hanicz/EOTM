package eye.on.the.money.service.forex.impl;

import eye.on.the.money.dto.out.ForexTransactionDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.forex.ForexTransaction;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.forex.ForexTransactionRepository;
import eye.on.the.money.service.api.CurrencyConverter;
import eye.on.the.money.service.forex.ForexTransactionService;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ForexTransactionServiceImpl implements ForexTransactionService {

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private ForexTransactionRepository forexTransactionRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private CurrencyConverter currencyConverter;

    @Override
    public List<ForexTransactionDTO> getForexTransactionsByUserId(Long userId) {
        return this.forexTransactionRepository.findByUser_IdOrderByTransactionDate(userId).stream().map(this::convertToForexTransactionDTO).collect(Collectors.toList());
    }

    private ForexTransactionDTO convertToForexTransactionDTO(ForexTransaction forexTransaction) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(forexTransaction, ForexTransactionDTO.class);
    }

    @Transactional
    @Override
    public void deleteForexTransactionById(User user, List<Long> ids) {
        this.forexTransactionRepository.deleteByUser_IdAndIdIn(user.getId(), ids);
    }

    @Transactional
    @Override
    public ForexTransactionDTO createForexTransaction(ForexTransactionDTO forexTransactionDTO, User user) {
        Currency toCurrency = this.currencyRepository.findById(forexTransactionDTO.getToCurrencyId()).orElseThrow(NoSuchElementException::new);
        Currency fromCurrency = this.currencyRepository.findById(forexTransactionDTO.getFromCurrencyId()).orElseThrow(NoSuchElementException::new);

        ForexTransaction forexTransaction = ForexTransaction.builder()
                .buySell(forexTransactionDTO.getBuySell())
                .transactionDate(forexTransactionDTO.getTransactionDate())
                .toCurrency(toCurrency)
                .fromCurrency(fromCurrency)
                .fromAmount(forexTransactionDTO.getFromAmount())
                .toAmount(forexTransactionDTO.getToAmount())
                .changeRate(forexTransactionDTO.getBuySell().equals("B") ? forexTransactionDTO.getFromAmount()/forexTransactionDTO.getToAmount() : forexTransactionDTO.getToAmount()/forexTransactionDTO.getFromAmount())
                .user(user)
                .build();

        forexTransaction = this.forexTransactionRepository.save(forexTransaction);
        return this.convertToForexTransactionDTO(forexTransaction);
    }

    @Transactional
    @Override
    public ForexTransactionDTO updateForexTransaction(ForexTransactionDTO forexTransactionDTO, User user) {
        Currency toCurrency = this.currencyRepository.findById(forexTransactionDTO.getToCurrencyId()).orElseThrow(NoSuchElementException::new);
        Currency fromCurrency = this.currencyRepository.findById(forexTransactionDTO.getFromCurrencyId()).orElseThrow(NoSuchElementException::new);
        ForexTransaction forexTransaction = this.forexTransactionRepository.findByIdAndUser_Id(forexTransactionDTO.getForexTransactionId(), user.getId()).orElseThrow(NoSuchElementException::new);

        forexTransaction.setBuySell(forexTransactionDTO.getBuySell());
        forexTransaction.setTransactionDate(forexTransactionDTO.getTransactionDate());
        forexTransaction.setFromAmount(forexTransactionDTO.getFromAmount());
        forexTransaction.setToAmount(forexTransactionDTO.getToAmount());
        forexTransaction.setToCurrency(toCurrency);
        forexTransaction.setFromCurrency(fromCurrency);
        forexTransaction.setChangeRate(forexTransactionDTO.getBuySell().equals("B") ? forexTransactionDTO.getFromAmount()/forexTransactionDTO.getToAmount() : forexTransactionDTO.getToAmount()/forexTransactionDTO.getFromAmount());

        return this.convertToForexTransactionDTO(forexTransaction);
    }

    @Override
    public List<ForexTransactionDTO> getAllForexHoldings(Long userId) {
        Map<String, ForexTransactionDTO> forexTransactionMap = this.getCalculated(userId);
        List<ForexTransactionDTO> forexTransactions = new ArrayList<>(forexTransactionMap.values());
        this.currencyConverter.changeLiveValueCurrencyForForexTransactions(forexTransactions);
        return forexTransactions;
    }

    private Map<String, ForexTransactionDTO> getCalculated(Long userId) {
        List<ForexTransactionDTO> forexTransactions = this.forexTransactionRepository.findByUser_IdOrderByTransactionDate(userId).stream().map(this::convertToForexTransactionDTO).collect(Collectors.toList());
        Map<String, ForexTransactionDTO> forexTransactionMap = new HashMap<>();
        for (ForexTransactionDTO ft : forexTransactions) {
            String symbol = ft.getFromCurrencyId() + ft.getToCurrencyId();
            forexTransactionMap.compute(symbol, (key, value) -> (value == null) ? ft : value.mergeTransactions(ft));
        }
        return forexTransactionMap;
    }
}