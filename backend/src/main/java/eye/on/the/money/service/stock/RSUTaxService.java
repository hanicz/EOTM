package eye.on.the.money.service.stock;

import eye.on.the.money.dto.in.RSUDTO;
import eye.on.the.money.dto.out.RSUTaxDTO;
import eye.on.the.money.dto.out.RSUTaxEventDTO;
import eye.on.the.money.dto.out.RSUTaxEventReportDTO;
import eye.on.the.money.dto.out.TaxBreakdownDTO;
import eye.on.the.money.model.financial.TaxDetails;
import eye.on.the.money.model.stock.Investment;
import eye.on.the.money.model.stock.RSUTaxDetails;
import eye.on.the.money.repository.stock.InvestmentRepository;
import eye.on.the.money.repository.stock.RSUTaxDetailsRepository;
import eye.on.the.money.service.shared.ICSVService;
import eye.on.the.money.service.shared.TaxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RSUTaxService implements ICSVService {

    private static final String BUY = "B";

    private final InvestmentRepository investmentRepository;
    private final RSUTaxDetailsRepository rsuTaxDetailsRepository;
    private final TaxService taxService;

    @Transactional
    public void setRSU(String userEmail, List<Long> ids, boolean rsu) {
        log.trace("Enter");
        List<Investment> investments = this.investmentRepository.findByUserEmailAndIdIn(userEmail, ids).stream()
                .filter(investment -> BUY.equals(investment.getBuySell())).toList();
        if (investments.isEmpty()) return;

        investments.forEach(investment -> investment.setRsu(rsu));
        this.investmentRepository.saveAll(investments);

        List<Long> investmentIds = investments.stream().map(Investment::getId).toList();
        if (!rsu) {
            this.rsuTaxDetailsRepository.deleteByInvestmentIdIn(investmentIds);
            return;
        }

        Map<Long, RSUTaxDetails> existing = this.rsuTaxDetailsRepository
                .findByUserEmailAndInvestmentIdIn(userEmail, investmentIds).stream()
                .collect(Collectors.toMap(details -> details.getInvestment().getId(), Function.identity()));

        List<RSUTaxDTO> valued = this.taxService.calculateTaxForRSUs(investments.stream()
                .map(this::toRSUDTO).toList()).getItems();

        List<RSUTaxDetails> details = new ArrayList<>();
        for (int index = 0; index < investments.size(); index++) {
            Investment investment = investments.get(index);
            details.add(this.toRSUTaxDetails(investment, valued.get(index), existing.get(investment.getId())));
        }
        this.rsuTaxDetailsRepository.saveAll(details);
    }

    @Transactional
    public void setTaxPaid(String userEmail, List<Long> ids, boolean paid) {
        log.trace("Enter");
        List<RSUTaxDetails> details = this.rsuTaxDetailsRepository
                .findByUserEmailAndInvestmentIdIn(userEmail, ids);
        if (details.isEmpty()) return;

        details.forEach(detail -> detail.getTaxDetails().setPaid(paid));
        this.rsuTaxDetailsRepository.saveAll(details);
    }

    public RSUTaxEventReportDTO getRSUTaxEvents(String userEmail) {
        log.trace("Enter");
        List<RSUTaxDetails> details = this.rsuTaxDetailsRepository.findFlaggedByUserEmail(userEmail);
        if (details.isEmpty()) return RSUTaxEventReportDTO.empty();

        List<RSUTaxEventDTO> items = details.stream().map(this::convertToDTO).toList();

        BigDecimal totalAmount = items.stream().map(RSUTaxEventDTO::getAmountInHuf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        TaxBreakdownDTO totalTax = items.stream().map(RSUTaxEventDTO::getTax)
                .reduce(TaxBreakdownDTO.zero(), TaxBreakdownDTO::plus);

        return RSUTaxEventReportDTO.builder().items(items)
                .totalAmountInHuf(totalAmount).totalTax(totalTax).build();
    }

    public void getCSV(String userEmail, Writer writer) {
        log.trace("Enter");
        this.printRecords(this.getRSUTaxEvents(userEmail).getItems(), writer);
    }

    private RSUDTO toRSUDTO(Investment investment) {
        return RSUDTO.builder()
                .shortName(investment.getStock().getShortName())
                .exchange(investment.getStock().getExchange())
                .date(investment.getTransactionDate())
                .quantity(investment.getQuantity())
                .build();
    }

    private RSUTaxDetails toRSUTaxDetails(Investment investment, RSUTaxDTO valued, RSUTaxDetails existing) {
        boolean paid = existing != null && existing.getTaxDetails() != null && existing.getTaxDetails().isPaid();
        TaxBreakdownDTO tax = valued.getTax();
        TaxDetails details = TaxDetails.builder()
                .rate(valued.getRate())
                .rateDate(valued.getRateDate())
                .amountInHuf(valued.getAmountInHuf())
                .taxBase(tax.getTaxBase())
                .szocho(tax.getSzocho())
                .szja(tax.getSzja())
                .total(tax.getTotal())
                .calculatedOn(LocalDate.now())
                .paid(paid)
                .build();

        RSUTaxDetails target = (existing != null) ? existing
                : RSUTaxDetails.builder().investment(investment).build();
        target.setPrice(valued.getPrice());
        target.setPriceDate(valued.getPriceDate());
        target.setCurrency(valued.getCurrency());
        target.setTaxDetails(details);
        return target;
    }

    private RSUTaxEventDTO convertToDTO(RSUTaxDetails rsuDetails) {
        Investment investment = rsuDetails.getInvestment();
        TaxDetails details = rsuDetails.getTaxDetails();
        return RSUTaxEventDTO.builder()
                .id(investment.getId())
                .shortName(investment.getStock().getShortName())
                .exchange(investment.getStock().getExchange())
                .transactionDate(investment.getTransactionDate())
                .quantity(investment.getQuantity())
                .currency(rsuDetails.getCurrency())
                .price(rsuDetails.getPrice())
                .priceDate(rsuDetails.getPriceDate())
                .amount(this.value(rsuDetails.getPrice(), investment.getQuantity()))
                .rate(details.getRate())
                .rateDate(details.getRateDate())
                .amountInHuf(details.getAmountInHuf())
                .calculatedOn(details.getCalculatedOn())
                .paid(details.isPaid())
                .tax(TaxBreakdownDTO.builder()
                        .amount(details.getAmountInHuf())
                        .taxBase(details.getTaxBase())
                        .szocho(details.getSzocho())
                        .szja(details.getSzja())
                        .total(details.getTotal())
                        .build())
                .build();
    }

    private BigDecimal value(BigDecimal price, Integer quantity) {
        if (price == null || quantity == null) return BigDecimal.ZERO;
        return price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }
}
