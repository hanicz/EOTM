package eye.on.the.money.service.stock;

import eye.on.the.money.dto.in.RSUDTO;
import eye.on.the.money.dto.in.RSUGrantEditDTO;
import eye.on.the.money.dto.out.RSUGrantDTO;
import eye.on.the.money.dto.out.RSUGrantReportDTO;
import eye.on.the.money.dto.out.RSUTaxDTO;
import eye.on.the.money.dto.out.RSUVestDTO;
import eye.on.the.money.dto.out.TaxBreakdownDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.exception.TaxException;
import eye.on.the.money.model.stock.RSUGrant;
import eye.on.the.money.model.stock.VestingFrequency;
import eye.on.the.money.repository.stock.RSUGrantRepository;
import eye.on.the.money.service.shared.ICSVService;
import eye.on.the.money.service.shared.TaxService;
import eye.on.the.money.service.user.UserService;
import eye.on.the.money.util.Ticker;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Writer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RSUGrantService implements ICSVService {

    private static final String DEFAULT_EXCHANGE = "US";

    private final RSUGrantRepository rsuGrantRepository;
    private final TaxService taxService;
    private final UserService userService;

    @Cacheable(cacheNames = "grants-rsu", key = "#userId")
    public RSUGrantReportDTO getGrants(Long userId) {
        List<RSUGrant> grants = this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(userId);
        if (grants.isEmpty()) return RSUGrantReportDTO.empty();

        return this.report(this.price(grants, LocalDate.now()));
    }

    @Transactional
    @CacheEvict(cacheNames = "grants-rsu", key = "#userId")
    public RSUGrantDTO createGrant(Long userId, RSUGrantEditDTO editDTO) {
        RSUGrant grant = RSUGrant.builder()
                .shortName(Ticker.normalizeShortName(editDTO.shortName()))
                .exchange(this.exchange(editDTO.exchange()))
                .currency(this.currency(editDTO.currency()))
                .grantDate(editDTO.grantDate())
                .quantity(editDTO.quantity())
                .vestingYears(editDTO.vestingYears())
                .vestingFrequency(editDTO.vestingFrequency())
                .note(this.trimToNull(editDTO.note()))
                .user(this.userService.getReference(userId))
                .build();

        return this.priceOne(this.rsuGrantRepository.saveAndFlush(grant));
    }

    @Transactional
    @CacheEvict(cacheNames = "grants-rsu", key = "#userId")
    public RSUGrantDTO updateGrant(Long userId, Long id, RSUGrantEditDTO editDTO) {
        RSUGrant grant = this.rsuGrantRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("RSU grant not found: " + id));

        grant.setShortName(Ticker.normalizeShortName(editDTO.shortName()));
        grant.setExchange(this.exchange(editDTO.exchange()));
        grant.setCurrency(this.currency(editDTO.currency()));
        grant.setGrantDate(editDTO.grantDate());
        grant.setQuantity(editDTO.quantity());
        grant.setVestingYears(editDTO.vestingYears());
        grant.setVestingFrequency(editDTO.vestingFrequency());
        grant.setNote(this.trimToNull(editDTO.note()));

        return this.priceOne(this.rsuGrantRepository.saveAndFlush(grant));
    }

    @Transactional
    @CacheEvict(cacheNames = "grants-rsu", key = "#userId")
    public void deleteGrantsByIds(Long userId, List<Long> ids) {
        this.rsuGrantRepository.deleteByUserIdAndIdIn(userId, ids);
    }

    public void getCSV(Long userId, Writer writer) {
        this.printRecords(this.getGrants(userId).getItems().stream()
                .flatMap(grant -> grant.getVests().stream()).toList(), writer);
    }

    private RSUGrantDTO priceOne(RSUGrant grant) {
        LocalDate today = LocalDate.now();
        List<Tranche> schedule = this.schedule(grant);
        List<RSUTaxDTO> valued = this.taxService.calculateTaxForRSUs(schedule.stream()
                .map(tranche -> this.toRSUDTO(grant, tranche, today)).toList()).getItems();

        return this.convertToDTO(grant, schedule, valued, today, null);
    }

    private List<RSUGrantDTO> price(List<RSUGrant> grants, LocalDate today) {
        List<List<Tranche>> schedules = grants.stream().map(this::schedule).toList();

        List<RSUDTO> requests = new ArrayList<>();
        for (int index = 0; index < grants.size(); index++) {
            RSUGrant grant = grants.get(index);
            schedules.get(index).forEach(tranche -> requests.add(this.toRSUDTO(grant, tranche, today)));
        }

        List<RSUTaxDTO> valued = this.valueOrNull(requests);
        if (valued != null) {
            return this.zip(grants, schedules, valued, today);
        }
        return this.priceSeparately(grants, schedules, today);
    }

    private List<RSUGrantDTO> zip(List<RSUGrant> grants, List<List<Tranche>> schedules,
                                  List<RSUTaxDTO> valued, LocalDate today) {
        List<RSUGrantDTO> priced = new ArrayList<>();
        int cursor = 0;
        for (int index = 0; index < grants.size(); index++) {
            List<Tranche> schedule = schedules.get(index);
            priced.add(this.convertToDTO(grants.get(index), schedule,
                    valued.subList(cursor, cursor + schedule.size()), today, null));
            cursor += schedule.size();
        }
        return priced;
    }

    private List<RSUGrantDTO> priceSeparately(List<RSUGrant> grants, List<List<Tranche>> schedules,
                                              LocalDate today) {
        List<RSUGrantDTO> priced = new ArrayList<>();
        for (int index = 0; index < grants.size(); index++) {
            RSUGrant grant = grants.get(index);
            List<Tranche> schedule = schedules.get(index);
            List<RSUTaxDTO> valued = this.valueOrNull(schedule.stream()
                    .map(tranche -> this.toRSUDTO(grant, tranche, today)).toList());

            priced.add(this.convertToDTO(grant, schedule, (valued == null) ? List.of() : valued, today,
                    (valued == null) ? "Could not value " + Ticker.symbol(grant.getShortName(),
                            grant.getExchange()) : null));
        }
        return priced;
    }

    private List<RSUTaxDTO> valueOrNull(List<RSUDTO> requests) {
        try {
            return this.taxService.calculateTaxForRSUs(requests).getItems();
        } catch (TaxException | APIException e) {
            return null;
        }
    }

    private List<Tranche> schedule(RSUGrant grant) {
        VestingFrequency frequency = grant.getVestingFrequency();
        int count = frequency.vestCount(grant.getVestingYears());
        long total = grant.getQuantity();

        List<Tranche> tranches = new ArrayList<>();
        long allocated = 0;
        for (int sequence = 1; sequence <= count; sequence++) {
            long cumulative = total * sequence / count;
            tranches.add(new Tranche(sequence,
                    grant.getGrantDate().plusMonths((long) sequence * frequency.getMonthsBetweenVests()),
                    (int) (cumulative - allocated)));
            allocated = cumulative;
        }
        return tranches;
    }

    private RSUDTO toRSUDTO(RSUGrant grant, Tranche tranche, LocalDate today) {
        return RSUDTO.builder()
                .shortName(grant.getShortName())
                .exchange(grant.getExchange())
                .currency(grant.getCurrency())
                .date(tranche.vestDate().isAfter(today) ? today : tranche.vestDate())
                .quantity(tranche.quantity())
                .build();
    }

    private RSUGrantDTO convertToDTO(RSUGrant grant, List<Tranche> schedule, List<RSUTaxDTO> valued,
                                     LocalDate today, String error) {
        List<RSUVestDTO> vests = new ArrayList<>();
        for (int index = 0; index < schedule.size(); index++) {
            RSUTaxDTO priced = (index < valued.size()) ? valued.get(index) : null;
            vests.add(this.toVestDTO(grant, schedule.get(index), priced, today));
        }

        BigDecimal totalAmountInHuf = vests.stream().map(RSUVestDTO::getAmountInHuf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        TaxBreakdownDTO totalTax = vests.stream().map(RSUVestDTO::getTax)
                .reduce(TaxBreakdownDTO.zero(), TaxBreakdownDTO::plus);

        return RSUGrantDTO.builder()
                .id(grant.getId())
                .shortName(grant.getShortName())
                .exchange(grant.getExchange())
                .currency(grant.getCurrency())
                .grantDate(grant.getGrantDate())
                .quantity(grant.getQuantity())
                .vestingYears(grant.getVestingYears())
                .vestingFrequency(grant.getVestingFrequency())
                .note(grant.getNote())
                .vests(vests)
                .totalAmountInHuf(totalAmountInHuf)
                .totalTax(totalTax)
                .totalNetInHuf(this.netOf(vests, null))
                .vestedNetInHuf(this.netOf(vests, true))
                .upcomingNetInHuf(this.netOf(vests, false))
                .error(error)
                .build();
    }

    private RSUVestDTO toVestDTO(RSUGrant grant, Tranche tranche, RSUTaxDTO priced, LocalDate today) {
        boolean vested = !tranche.vestDate().isAfter(today);
        TaxBreakdownDTO tax = (priced == null || priced.getTax() == null)
                ? TaxBreakdownDTO.zero() : priced.getTax();
        BigDecimal amountInHuf = (priced == null || priced.getAmountInHuf() == null)
                ? BigDecimal.ZERO : priced.getAmountInHuf();

        return RSUVestDTO.builder()
                .grantId(grant.getId())
                .shortName(grant.getShortName())
                .exchange(grant.getExchange())
                .grantDate(grant.getGrantDate())
                .sequence(tranche.sequence())
                .vestDate(tranche.vestDate())
                .quantity(tranche.quantity())
                .currency((priced == null) ? grant.getCurrency() : priced.getCurrency())
                .price((priced == null) ? null : priced.getPrice())
                .priceDate((priced == null) ? null : priced.getPriceDate())
                .amount((priced == null) ? BigDecimal.ZERO : priced.getAmount())
                .rate((priced == null) ? null : priced.getRate())
                .rateDate((priced == null) ? null : priced.getRateDate())
                .amountInHuf(amountInHuf)
                .netInHuf(amountInHuf.subtract(tax.getTotal()))
                .vested(vested)
                .projected(!vested)
                .tax(tax)
                .build();
    }

    private RSUGrantReportDTO report(List<RSUGrantDTO> items) {
        BigDecimal totalAmountInHuf = items.stream().map(RSUGrantDTO::getTotalAmountInHuf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        TaxBreakdownDTO totalTax = items.stream().map(RSUGrantDTO::getTotalTax)
                .reduce(TaxBreakdownDTO.zero(), TaxBreakdownDTO::plus);
        BigDecimal totalNetInHuf = items.stream().map(RSUGrantDTO::getTotalNetInHuf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return RSUGrantReportDTO.builder().items(items).totalAmountInHuf(totalAmountInHuf)
                .totalTax(totalTax).totalNetInHuf(totalNetInHuf).build();
    }

    private BigDecimal netOf(List<RSUVestDTO> vests, Boolean vested) {
        return vests.stream().filter(vest -> vested == null || vest.isVested() == vested)
                .map(RSUVestDTO::getNetInHuf).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String exchange(String exchange) {
        return (exchange == null || exchange.isBlank())
                ? DEFAULT_EXCHANGE : Ticker.normalizeExchange(exchange);
    }

    private String currency(String currency) {
        String trimmed = this.trimToNull(currency);
        return (trimmed == null) ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record Tranche(int sequence, LocalDate vestDate, int quantity) {
    }
}
