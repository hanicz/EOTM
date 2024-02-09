package eye.on.the.money.service.etf;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.etf.ETF;
import eye.on.the.money.model.etf.ETFInvestment;
import eye.on.the.money.model.etf.ETFPayment;
import eye.on.the.money.repository.etf.ETFInvestmentRepository;
import eye.on.the.money.repository.etf.ETFRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.service.shared.ICSVService;
import eye.on.the.money.service.user.UserServiceImpl;
import eye.on.the.money.service.api.EODAPIService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Writer;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ETFInvestmentService implements ICSVService {
    private final ETFInvestmentRepository etfInvestmentRepository;
    private final ETFRepository etfRepository;
    private final CurrencyRepository currencyRepository;
    private final EODAPIService eodAPIService;
    private final UserServiceImpl userService;
    private final ModelMapper modelMapper;
    private final ETFPaymentService etfPaymentService;

    public List<ETFInvestmentDTO> getETFInvestments(String userEmail) {
        return this.etfInvestmentRepository.findByUserEmailOrderByTransactionDate(userEmail).stream().map(this::convertToETFInvestmentDTO).collect(Collectors.toList());
    }

    private ETFInvestmentDTO convertToETFInvestmentDTO(ETFInvestment etfInvestment) {
        return this.modelMapper.map(etfInvestment, ETFInvestmentDTO.class);
    }

    public List<ETFInvestmentDTO> getCurrentETFHoldings(String userEmail) {
        Map<String, ETFInvestmentDTO> investmentMap = this.getCalculated(userEmail);
        List<ETFInvestmentDTO> etfInvestmentDTOList = (new ArrayList<>(investmentMap.values()))
                .stream().filter(i -> (i.getQuantity() > 0)).collect(Collectors.toList());
        String joinedList = etfInvestmentDTOList.stream().map(i -> (i.getShortName() + "." + i.getExchange())).collect(Collectors.joining(","));

        JsonNode responseBody = this.eodAPIService.getLiveValue(joinedList, "/real-time/etf/?api_token={0}&fmt=json&s={1}");

        for (JsonNode etf : responseBody) {
            Optional<ETFInvestmentDTO> etfInvestmentDTO = etfInvestmentDTOList.stream().filter
                    (i -> (i.getShortName() + "." + i.getExchange()).equals(etf.findValue("code").textValue())).findFirst();
            if (etfInvestmentDTO.isEmpty()) continue;
            etfInvestmentDTO.get().setLiveValue(etf.findValue("close").doubleValue() * etfInvestmentDTO.get().getQuantity());
            etfInvestmentDTO.get().setValueDiff(etfInvestmentDTO.get().getLiveValue() - etfInvestmentDTO.get().getAmount());
        }
        return etfInvestmentDTOList;
    }

    public List<ETFInvestmentDTO> getAllPositions(String userEmail) {
        Map<String, ETFInvestmentDTO> investmentMap = this.getCalculated(userEmail);
        return new ArrayList<>(investmentMap.values());
    }

    private Map<String, ETFInvestmentDTO> getCalculated(String userEmail) {
        List<ETFInvestmentDTO> investments = this.etfInvestmentRepository.findByUserEmailOrderByTransactionDate(userEmail).stream().map(this::convertToETFInvestmentDTO).toList();

        Map<String, ETFInvestmentDTO> investmentMap = new HashMap<>();
        for (ETFInvestmentDTO i : investments) {
            if (i.getBuySell().equals("S")) {
                i.negateAmountAndQuantity();
            }
            investmentMap.compute(i.getShortName(), (key, value) -> (value == null) ? i : value.mergeInvestments(i));
        }
        return investmentMap;
    }


    @Transactional
    public void deleteInvestmentById(String userEmail, String ids) {
        List<Long> idList = Stream.of(ids.split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        this.etfInvestmentRepository.deleteByUserEmailAndIdIn(userEmail, idList);
    }

    @Transactional
    public ETFInvestmentDTO createInvestment(ETFInvestmentDTO investmentDTO, String userEmail) {
        Currency currency = this.currencyRepository.findById(investmentDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        ETF etf = this.etfRepository.findByShortNameAndExchange(investmentDTO.getShortName(), investmentDTO.getExchange()).orElseThrow(NoSuchElementException::new);
        ETFPayment etfPayment = this.etfPaymentService.createPayment(currency, investmentDTO.getAmount());
        User user = this.userService.loadUserByEmail(userEmail);

        ETFInvestment investment = ETFInvestment.builder()
                .buySell(investmentDTO.getBuySell())
                .creationDate(LocalDate.now())
                .transactionDate(investmentDTO.getTransactionDate())
                .user(user)
                .quantity(investmentDTO.getQuantity())
                .etf(etf)
                .etfPayment(etfPayment)
                .fee(investmentDTO.getFee())
                .build();
        investment = this.etfInvestmentRepository.save(investment);
        return this.convertToETFInvestmentDTO(investment);
    }

    @Transactional
    public ETFInvestmentDTO updateInvestment(ETFInvestmentDTO investmentDTO, String userEmail) {
        Currency currency = this.currencyRepository.findById(investmentDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        ETF etf = this.etfRepository.findByShortNameAndExchange(investmentDTO.getShortName(), investmentDTO.getExchange()).orElseThrow(NoSuchElementException::new);
        ETFInvestment investment = this.etfInvestmentRepository.findByIdAndUserEmail(investmentDTO.getId(), userEmail).orElseThrow(NoSuchElementException::new);
        ETFPayment etfPayment = investment.getEtfPayment();

        investment.setBuySell(investmentDTO.getBuySell());
        investment.setTransactionDate(investmentDTO.getTransactionDate());
        investment.setQuantity(investmentDTO.getQuantity());
        investment.setEtf(etf);
        investment.setFee(investmentDTO.getFee());
        etfPayment.setAmount(investmentDTO.getAmount());
        etfPayment.setCurrency(currency);

        return this.convertToETFInvestmentDTO(investment);
    }

    public void getCSV(String userEmail, Writer writer) {
        List<ETFInvestmentDTO> investmentList =
                this.etfInvestmentRepository.findByUserEmailOrderByTransactionDate(userEmail)
                        .stream()
                        .map(this::convertToETFInvestmentDTO).
                        toList();
        this.printRecords(investmentList, writer);
    }
}
