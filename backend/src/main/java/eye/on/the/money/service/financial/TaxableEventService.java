package eye.on.the.money.service.financial;

import eye.on.the.money.dto.out.TaxBreakdownDTO;
import eye.on.the.money.dto.out.TaxableEventDTO;
import eye.on.the.money.dto.out.TaxableEventReportDTO;
import eye.on.the.money.exception.TaxException;
import eye.on.the.money.model.financial.BankTransaction;
import eye.on.the.money.model.financial.TaxDetails;
import eye.on.the.money.repository.financial.BankTransactionRepository;
import eye.on.the.money.service.api.MNBAPIService;
import eye.on.the.money.service.shared.ICSVService;
import eye.on.the.money.service.shared.TaxCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Taxes the bank transactions the user flagged as taxable events, with the same tax method as the tax page.
 * <p>
 * The tax is worked out once, when the transaction is flagged, and stored with it - flagging an already
 * flagged transaction again is what recalculates it. The report is then a plain read, so it neither depends
 * on MNB being reachable nor changes under the user when a rate is revised.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TaxableEventService implements ICSVService {

    /** MNB skips non-banking days; look back far enough to clear a long holiday. */
    private static final int LOOKBACK_DAYS = 14;

    private final BankTransactionRepository bankTransactionRepository;
    private final MNBAPIService mnbAPIService;
    private final TaxCalculator taxCalculator;

    @Transactional
    public void setTaxable(String userEmail, List<Long> ids, boolean taxable) {
        log.trace("Enter");
        List<BankTransaction> transactions = this.bankTransactionRepository.findByUserEmailAndIdIn(userEmail, ids);
        if (transactions.isEmpty()) return;

        Map<String, NavigableMap<LocalDate, BigDecimal>> rates = taxable ? this.fetchRates(transactions) : Map.of();
        for (BankTransaction transaction : transactions) {
            transaction.setTaxable(taxable);
            transaction.setTaxDetails(taxable ? this.calculate(transaction, rates) : null);
        }
        this.bankTransactionRepository.saveAll(transactions);
    }

    public TaxableEventReportDTO getTaxableEvents(String userEmail) {
        log.trace("Enter");
        List<BankTransaction> transactions =
                this.bankTransactionRepository.findByUserEmailAndTaxableTrueOrderByBookingDateDesc(userEmail);
        if (transactions.isEmpty()) return TaxableEventReportDTO.empty();

        List<TaxableEventDTO> items = transactions.stream().map(this::convertToDTO).toList();

        BigDecimal totalAmount = items.stream().map(TaxableEventDTO::getAmountInHuf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        TaxBreakdownDTO totalTax = items.stream().map(TaxableEventDTO::getTax)
                .reduce(TaxBreakdownDTO.zero(), TaxBreakdownDTO::plus);

        return TaxableEventReportDTO.builder().items(items)
                .totalAmountInHuf(totalAmount).totalTax(totalTax).build();
    }

    public void getCSV(String userEmail, Writer writer) {
        log.trace("Enter");
        this.printRecords(this.getTaxableEvents(userEmail).getItems(), writer);
    }

    private TaxDetails calculate(BankTransaction transaction,
                                 Map<String, NavigableMap<LocalDate, BigDecimal>> rates) {
        Map.Entry<LocalDate, BigDecimal> rate = this.rateOn(rates, transaction.getCurrency().getId(),
                transaction.getBookingDate());
        BigDecimal amountInHuf = BigDecimal.valueOf(transaction.getAmount())
                .multiply(rate.getValue()).setScale(2, RoundingMode.HALF_UP);
        TaxBreakdownDTO tax = this.taxCalculator.calculateTax(amountInHuf);

        return TaxDetails.builder()
                .rate(rate.getValue())
                .rateDate(rate.getKey())
                .amountInHuf(amountInHuf)
                .taxBase(tax.getTaxBase())
                .szocho(tax.getSzocho())
                .szja(tax.getSzja())
                .total(tax.getTotal())
                .calculatedOn(LocalDate.now())
                .build();
    }

    private TaxableEventDTO convertToDTO(BankTransaction transaction) {
        TaxDetails details = transaction.getTaxDetails();
        return TaxableEventDTO.builder()
                .id(transaction.getId())
                .bookingDate(transaction.getBookingDate())
                .type(transaction.getType())
                .partnerName(transaction.getPartnerName())
                .memo(transaction.getMemo())
                .amount(BigDecimal.valueOf(transaction.getAmount()).setScale(2, RoundingMode.HALF_UP))
                .currencyId(transaction.getCurrency().getId())
                .rate(details.getRate())
                .rateDate(details.getRateDate())
                .amountInHuf(details.getAmountInHuf())
                .calculatedOn(details.getCalculatedOn())
                .tax(TaxBreakdownDTO.builder()
                        .amount(details.getAmountInHuf())
                        .taxBase(details.getTaxBase())
                        .szocho(details.getSzocho())
                        .szja(details.getSzja())
                        .total(details.getTotal())
                        .build())
                .build();
    }

    private Map<String, NavigableMap<LocalDate, BigDecimal>> fetchRates(List<BankTransaction> transactions) {
        Set<String> needed = transactions.stream()
                .map(transaction -> transaction.getCurrency().getId())
                .filter(currency -> !MNBAPIService.HUF.equalsIgnoreCase(currency))
                .collect(Collectors.toSet());
        if (needed.isEmpty()) return Map.of();

        LocalDate earliest = transactions.stream().map(BankTransaction::getBookingDate)
                .min(LocalDate::compareTo).orElseThrow();
        LocalDate latest = transactions.stream().map(BankTransaction::getBookingDate)
                .max(LocalDate::compareTo).orElseThrow();

        return this.mnbAPIService.getExchangeRates(needed, earliest.minusDays(LOOKBACK_DAYS), latest);
    }

    private Map.Entry<LocalDate, BigDecimal> rateOn(Map<String, NavigableMap<LocalDate, BigDecimal>> rates,
                                                    String currency, LocalDate date) {
        if (MNBAPIService.HUF.equalsIgnoreCase(currency)) return Map.entry(date, BigDecimal.ONE);

        NavigableMap<LocalDate, BigDecimal> forCurrency = rates.get(currency.toUpperCase());
        Map.Entry<LocalDate, BigDecimal> entry = (forCurrency == null) ? null : forCurrency.floorEntry(date);
        if (entry == null) {
            throw new TaxException("No MNB rate published for " + currency + " on or before " + date);
        }
        return entry;
    }
}
