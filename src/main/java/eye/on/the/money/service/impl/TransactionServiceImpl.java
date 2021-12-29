package eye.on.the.money.service.impl;

import eye.on.the.money.dto.in.InvestmentQuery;
import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.crypto.Payment;
import eye.on.the.money.model.User;
import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.model.crypto.Transaction;
import eye.on.the.money.repository.CoinRepository;
import eye.on.the.money.repository.CurrencyRepository;
import eye.on.the.money.repository.TransactionRepository;
import eye.on.the.money.service.PaymentService;
import eye.on.the.money.service.TransactionService;
import eye.on.the.money.service.currency.CryptoAPIService;
import eye.on.the.money.service.currency.CurrencyConverter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private CurrencyConverter currencyConverter;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private CryptoAPIService cryptoAPIService;

    @Override
    public List<TransactionDTO> getTransactionsByUserId(Long userId) {
        return this.transactionRepository.findByUser_IdOrderByTransactionDate(userId).stream().map(this::convertToTransactionDTO).collect(Collectors.toList());
    }

    @Override
    public List<TransactionDTO> getTransactionsByUserIdWConvCurr(Long userId, String currency) {
        List<TransactionDTO> transactions =
                this.transactionRepository.findByUser_IdOrderByTransactionDate(userId).stream().map(this::convertToTransactionDTO).collect(Collectors.toList());
        this.currencyConverter.changeTransactionsCurrency(transactions, currency);
        return transactions;
    }

    @Override
    public List<TransactionDTO> getAllPositions(Long userId, TransactionQuery query) {
        Map<String, TransactionDTO> transactionMap = this.getCalculated(userId, query);
        return (new ArrayList<TransactionDTO>(transactionMap.values())).stream().map(transaction -> {
            transaction.setCurrencyId(query.getCurrency());
            return transaction;
        }).collect(Collectors.toList());
    }

    @Override
    public List<TransactionDTO> getCurrentHoldings(Long userId, TransactionQuery query) {
        Map<String, TransactionDTO> transactionMap = this.getCalculated(userId, query);
        List<TransactionDTO> transactionDTOList = (new ArrayList<TransactionDTO>(transactionMap.values()))
                .stream().filter(i -> (i.getQuantity() > 0)).collect(Collectors.toList());
        this.cryptoAPIService.getLiveValue(transactionDTOList, query.getCurrency());
        this.currencyConverter.changeTransactionsCurrency(transactionDTOList, query.getCurrency());
        return transactionDTOList;
    }

    private TransactionDTO convertToTransactionDTO(Transaction transaction) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(transaction, TransactionDTO.class);
    }

    @Override
    public void deleteTransactionById(Long id) {
        this.transactionRepository.deleteById(id);
    }

    @Transactional
    public TransactionDTO createTransaction(TransactionDTO transactionDTO, User user) {
        Currency currency = this.currencyRepository.findByName(transactionDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        Coin coin = this.coinRepository.findByName(transactionDTO.getSymbol()).orElseThrow(NoSuchElementException::new);
        Payment payment = this.paymentService.createPayment(currency, transactionDTO.getAmount());

        Transaction transaction = Transaction.builder()
                .buySell(transactionDTO.getBuySell())
                .transactionDate(transactionDTO.getTransactionDate())
                .creationDate(new Date())
                .coin(coin)
                .payment(payment)
                .user(user)
                .build();
        transaction = this.transactionRepository.save(transaction);
        return this.convertToTransactionDTO(transaction);
    }

    private Map<String, TransactionDTO> getCalculated(Long userId, TransactionQuery query) {
        List<TransactionDTO> transactions = this.transactionRepository.findByUser_IdOrderByTransactionDate(userId).stream().map(this::convertToTransactionDTO).collect(Collectors.toList());
        this.currencyConverter.changeTransactionsCurrency(transactions, query.getCurrency());
        Map<String, TransactionDTO> transactionMap = new HashMap<>();
        for (TransactionDTO t : transactions) {
            if (t.getBuySell().equals("S")) {
                t.negateAmountAndQuantity();
            }
            transactionMap.compute(t.getSymbol(), (key, value) -> (value == null) ? t : value.mergeTransactions(t));
        }
        return transactionMap;
    }
}
