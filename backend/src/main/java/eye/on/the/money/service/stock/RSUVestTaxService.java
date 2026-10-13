package eye.on.the.money.service.stock;

import eye.on.the.money.dto.in.RSUDTO;
import eye.on.the.money.dto.out.RSUTaxDTO;
import eye.on.the.money.dto.out.RSUVestTaxEventDTO;
import eye.on.the.money.dto.out.RSUVestTaxEventReportDTO;
import eye.on.the.money.dto.out.TaxBreakdownDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.exception.TaxException;
import eye.on.the.money.model.financial.TaxDetails;
import eye.on.the.money.model.stock.RSUGrant;
import eye.on.the.money.model.stock.RSUVestTaxDetails;
import eye.on.the.money.model.stock.RSUVestingSchedule;
import eye.on.the.money.model.stock.RSUVestingSchedule.Tranche;
import eye.on.the.money.repository.stock.RSUGrantRepository;
import eye.on.the.money.repository.stock.RSUVestTaxDetailsRepository;
import eye.on.the.money.service.shared.ICSVService;
import eye.on.the.money.service.shared.TaxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RSUVestTaxService implements ICSVService {

    private final RSUGrantRepository rsuGrantRepository;
    private final RSUVestTaxDetailsRepository rsuVestTaxDetailsRepository;
    private final TaxService taxService;

    @Transactional
    public RSUVestTaxEventReportDTO getVestTaxEvents(Long userId) {
        Map<VestKey, Vest> vested = this.vestedTranches(userId, LocalDate.now());
        List<RSUVestTaxDetails> stored = this.rsuVestTaxDetailsRepository.findByUserId(userId);

        List<RSUVestTaxDetails> orphans = stored.stream()
                .filter(details -> !vested.containsKey(this.key(details)) && !details.getTaxDetails().isPaid())
                .toList();
        if (!orphans.isEmpty()) {
            this.rsuVestTaxDetailsRepository.deleteAll(orphans);
        }

        List<RSUVestTaxDetails> rows = new ArrayList<>(stored);
        rows.removeAll(orphans);

        Set<VestKey> known = rows.stream().map(this::key).collect(Collectors.toSet());
        List<Vest> missing = vested.entrySet().stream()
                .filter(entry -> !known.contains(entry.getKey())).map(Map.Entry::getValue).toList();

        Frozen frozen = this.freeze(missing);
        rows.addAll(frozen.rows());

        if (rows.isEmpty() && frozen.error() == null) return RSUVestTaxEventReportDTO.empty();

        List<RSUVestTaxEventDTO> items = rows.stream()
                .sorted(Comparator.comparing(RSUVestTaxDetails::getVestDate).reversed()
                        .thenComparing(details -> details.getGrant().getId()))
                .map(this::convertToDTO).toList();

        BigDecimal totalAmount = items.stream().map(RSUVestTaxEventDTO::getAmountInHuf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        TaxBreakdownDTO totalTax = items.stream().map(RSUVestTaxEventDTO::getTax)
                .reduce(TaxBreakdownDTO.zero(), TaxBreakdownDTO::plus);

        return RSUVestTaxEventReportDTO.builder().items(items).totalAmountInHuf(totalAmount)
                .totalTax(totalTax).error(frozen.error()).build();
    }

    @Transactional
    public void setTaxPaid(Long userId, List<Long> ids, boolean paid) {
        List<RSUVestTaxDetails> details = this.rsuVestTaxDetailsRepository.findByUserIdAndIdIn(userId, ids);
        if (details.isEmpty()) return;

        details.forEach(detail -> detail.getTaxDetails().setPaid(paid));
        this.rsuVestTaxDetailsRepository.saveAll(details);
    }

    @Transactional
    public void recalculate(Long userId, List<Long> ids) {
        List<RSUVestTaxDetails> details = this.rsuVestTaxDetailsRepository.findByUserIdAndIdIn(userId, ids);

        List<RSUVestTaxDetails> targets = new ArrayList<>();
        List<Vest> vests = new ArrayList<>();
        for (RSUVestTaxDetails detail : details) {
            RSUVestingSchedule.tranches(detail.getGrant()).stream()
                    .filter(tranche -> tranche.vestDate().equals(detail.getVestDate()))
                    .findFirst()
                    .ifPresent(tranche -> {
                        targets.add(detail);
                        vests.add(new Vest(detail.getGrant(), tranche));
                    });
        }
        if (targets.isEmpty()) return;

        List<RSUTaxDTO> valued = this.value(vests);
        for (int index = 0; index < targets.size(); index++) {
            this.apply(targets.get(index), vests.get(index).tranche(), valued.get(index));
        }
        this.rsuVestTaxDetailsRepository.saveAll(targets);
    }

    public void getCSV(Long userId, Writer writer) {
        this.printRecords(this.getVestTaxEvents(userId).getItems(), writer);
    }

    private Map<VestKey, Vest> vestedTranches(Long userId, LocalDate today) {
        Map<VestKey, Vest> vested = new LinkedHashMap<>();
        for (RSUGrant grant : this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(userId)) {
            RSUVestingSchedule.tranches(grant).stream()
                    .filter(tranche -> tranche.vestDate().isBefore(today))
                    .forEach(tranche -> vested.put(new VestKey(grant.getId(), tranche.vestDate()),
                            new Vest(grant, tranche)));
        }
        return vested;
    }

    private Frozen freeze(List<Vest> missing) {
        if (missing.isEmpty()) return new Frozen(List.of(), null);

        try {
            return new Frozen(this.save(missing, this.value(missing)), null);
        } catch (TaxException | APIException e) {
            return this.freezeByGrant(missing);
        }
    }

    private Frozen freezeByGrant(List<Vest> missing) {
        Map<Long, List<Vest>> byGrant = missing.stream()
                .collect(Collectors.groupingBy(vest -> vest.grant().getId(), LinkedHashMap::new, Collectors.toList()));

        List<RSUVestTaxDetails> rows = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (List<Vest> vests : byGrant.values()) {
            try {
                rows.addAll(this.save(vests, this.value(vests)));
            } catch (TaxException | APIException e) {
                RSUGrant grant = vests.getFirst().grant();
                failures.add(grant.getShortName() + " granted " + grant.getGrantDate());
            }
        }
        String error = failures.isEmpty() ? null
                : "Some vests could not be valued yet: " + String.join(", ", failures);
        return new Frozen(rows, error);
    }

    private List<RSUTaxDTO> value(List<Vest> vests) {
        return this.taxService.calculateTaxForRSUs(vests.stream().map(this::toRSUDTO).toList()).getItems();
    }

    private List<RSUVestTaxDetails> save(List<Vest> vests, List<RSUTaxDTO> valued) {
        List<RSUVestTaxDetails> rows = new ArrayList<>();
        for (int index = 0; index < vests.size(); index++) {
            Vest vest = vests.get(index);
            RSUVestTaxDetails row = RSUVestTaxDetails.builder().grant(vest.grant())
                    .vestDate(vest.tranche().vestDate()).build();
            this.apply(row, vest.tranche(), valued.get(index));
            rows.add(row);
        }
        return this.rsuVestTaxDetailsRepository.saveAll(rows);
    }

    private void apply(RSUVestTaxDetails target, Tranche tranche, RSUTaxDTO valued) {
        boolean paid = target.getTaxDetails() != null && target.getTaxDetails().isPaid();
        TaxBreakdownDTO tax = valued.getTax();

        target.setSequence(tranche.sequence());
        target.setQuantity(tranche.quantity());
        target.setPrice(valued.getPrice());
        target.setPriceDate(valued.getPriceDate());
        target.setCurrency(valued.getCurrency());
        target.setTaxDetails(TaxDetails.builder()
                .rate(valued.getRate())
                .rateDate(valued.getRateDate())
                .amountInHuf(valued.getAmountInHuf())
                .taxBase(tax.getTaxBase())
                .szocho(tax.getSzocho())
                .szja(tax.getSzja())
                .total(tax.getTotal())
                .calculatedOn(LocalDate.now())
                .paid(paid)
                .build());
    }

    private RSUDTO toRSUDTO(Vest vest) {
        return RSUDTO.builder()
                .shortName(vest.grant().getShortName())
                .exchange(vest.grant().getExchange())
                .currency(vest.grant().getCurrency())
                .date(vest.tranche().vestDate())
                .quantity(BigDecimal.valueOf(vest.tranche().quantity()))
                .build();
    }

    private RSUVestTaxEventDTO convertToDTO(RSUVestTaxDetails row) {
        RSUGrant grant = row.getGrant();
        TaxDetails details = row.getTaxDetails();
        return RSUVestTaxEventDTO.builder()
                .id(row.getId())
                .grantId(grant.getId())
                .shortName(grant.getShortName())
                .exchange(grant.getExchange())
                .grantDate(grant.getGrantDate())
                .sequence(row.getSequence())
                .vestDate(row.getVestDate())
                .quantity(row.getQuantity())
                .currency(row.getCurrency())
                .price(row.getPrice())
                .priceDate(row.getPriceDate())
                .amount(this.value(row.getPrice(), row.getQuantity()))
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

    private BigDecimal value(BigDecimal price, int quantity) {
        if (price == null) return BigDecimal.ZERO;
        return price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    private VestKey key(RSUVestTaxDetails details) {
        return new VestKey(details.getGrant().getId(), details.getVestDate());
    }

    private record VestKey(Long grantId, LocalDate vestDate) {
    }

    private record Vest(RSUGrant grant, Tranche tranche) {
    }

    private record Frozen(List<RSUVestTaxDetails> rows, String error) {
    }
}
