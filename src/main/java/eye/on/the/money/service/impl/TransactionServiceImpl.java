package eye.on.the.money.service.impl;

import eye.on.the.money.dto.TransactionDTO;
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
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
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
    private CoinRepository coinRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public List<TransactionDTO> getTransactionsByUserId(Long userId) {
        return this.transactionRepository.findByUser_Id(userId).stream().map(this::convertToTransactionDTO).collect(Collectors.toList());
    }

    private TransactionDTO convertToTransactionDTO(Transaction transaction) {
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return modelMapper.map(transaction, TransactionDTO.class);
    }

    @Override
    public void deleteTransactionById(Long id) {
        this.transactionRepository.deleteById(id);
    }

    @Transactional
    public TransactionDTO createTransaction(TransactionDTO transactionDTO, User user) {
        Currency currency = this.currencyRepository.findByName(transactionDTO.getCurrencyName()).orElseThrow(NoSuchElementException::new);
        Coin coin = this.coinRepository.findByName(transactionDTO.getName()).orElseThrow(NoSuchElementException::new);
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
}
