package eye.on.the.money.service.stock;

import eye.on.the.money.dto.in.STARGrantEditDTO;
import eye.on.the.money.dto.out.STARGrantDTO;
import eye.on.the.money.dto.out.STARGrantReportDTO;
import eye.on.the.money.dto.out.STARVestDTO;
import eye.on.the.money.dto.out.TaxBreakdownDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.stock.STARGrant;
import eye.on.the.money.model.stock.StarVestingSchedule;
import eye.on.the.money.repository.stock.STARGrantRepository;
import eye.on.the.money.service.api.MNBAPIService;
import eye.on.the.money.service.shared.TaxService;
import eye.on.the.money.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class STARGrantService {

    private static final int LOOKBACK_DAYS = 14;
    private static final int AMOUNT_SCALE = 2;

    private final STARGrantRepository starGrantRepository;
    private final MNBAPIService mnbAPIService;
    private final TaxService taxService;
    private final UserService userService;

    @Cacheable(cacheNames = "grants-star", key = "#userId")
    public STARGrantReportDTO getGrants(Long userId) {
        List<STARGrant> grants = this.starGrantRepository.findByUserIdOrderByCommencementDateDescIdAsc(userId);
        if (grants.isEmpty()) return STARGrantReportDTO.empty();

        LocalDate today = LocalDate.now();
        Map<String, Map.Entry<LocalDate, BigDecimal>> rates = this.rates(grants, today);

        return this.report(grants.stream().map(grant -> this.convertToDTO(grant, rates, today)).toList());
    }

    @Transactional
    @CacheEvict(cacheNames = "grants-star", key = "#userId")
    public STARGrantDTO createGrant(Long userId, STARGrantEditDTO editDTO) {
        STARGrant grant = STARGrant.builder()
                .name(this.name(editDTO.name()))
                .currency(this.currency(editDTO.currency()))
                .commencementDate(StarVestingSchedule.nextCommencementDate(editDTO.commencementDate()))
                .quantity(editDTO.quantity())
                .baseValue(editDTO.baseValue())
                .currentValue(editDTO.currentValue())
                .vestingYears(editDTO.vestingYears())
                .note(this.trimToNull(editDTO.note()))
                .user(this.userService.getReference(userId))
                .build();

        return this.saveAndPrice(userId, grant, editDTO.applyValueToAll());
    }

    @Transactional
    @CacheEvict(cacheNames = "grants-star", key = "#userId")
    public STARGrantDTO updateGrant(Long userId, Long id, STARGrantEditDTO editDTO) {
        STARGrant grant = this.starGrantRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("STAR grant not found: " + id));

        grant.setName(this.name(editDTO.name()));
        grant.setCurrency(this.currency(editDTO.currency()));
        grant.setCommencementDate(StarVestingSchedule.nextCommencementDate(editDTO.commencementDate()));
        grant.setQuantity(editDTO.quantity());
        grant.setBaseValue(editDTO.baseValue());
        grant.setCurrentValue(editDTO.currentValue());
        grant.setVestingYears(editDTO.vestingYears());
        grant.setNote(this.trimToNull(editDTO.note()));

        return this.saveAndPrice(userId, grant, editDTO.applyValueToAll());
    }

    @Transactional
    @CacheEvict(cacheNames = "grants-star", key = "#userId")
    public void deleteGrantsByIds(Long userId, List<Long> ids) {
        this.starGrantRepository.deleteByUserIdAndIdIn(userId, ids);
    }

    private STARGrantDTO saveAndPrice(Long userId, STARGrant grant, boolean applyValueToAll) {
        STARGrant saved = this.starGrantRepository.saveAndFlush(grant);
        if (applyValueToAll) {
            this.starGrantRepository.updateCurrentValue(userId, saved.getName(), saved.getCurrentValue());
        }

        LocalDate today = LocalDate.now();
        return this.convertToDTO(saved, this.rates(List.of(saved), today), today);
    }

    private Map<String, Map.Entry<LocalDate, BigDecimal>> rates(List<STARGrant> grants, LocalDate today) {
        Set<String> currencies = grants.stream().map(STARGrant::getCurrency)
                .filter(currency -> !MNBAPIService.HUF.equalsIgnoreCase(currency))
                .collect(Collectors.toSet());
        if (currencies.isEmpty()) return Map.of();

        Map<String, Map.Entry<LocalDate, BigDecimal>> latest = new HashMap<>();
        try {
            this.mnbAPIService.getExchangeRates(currencies, today.minusDays(LOOKBACK_DAYS), today)
                    .forEach((currency, series) -> {
                        Map.Entry<LocalDate, BigDecimal> entry = series.floorEntry(today);
                        if (entry != null) latest.put(currency.toUpperCase(Locale.ROOT), entry);
                    });
        } catch (APIException e) {
            return Map.of();
        }
        return latest;
    }

    private Map.Entry<LocalDate, BigDecimal> rateOf(STARGrant grant,
                                                    Map<String, Map.Entry<LocalDate, BigDecimal>> rates,
                                                    LocalDate today) {
        if (MNBAPIService.HUF.equalsIgnoreCase(grant.getCurrency())) {
            return Map.entry(today, BigDecimal.ONE);
        }
        return rates.get(grant.getCurrency().toUpperCase(Locale.ROOT));
    }

    private STARGrantDTO convertToDTO(STARGrant grant, Map<String, Map.Entry<LocalDate, BigDecimal>> rates,
                                      LocalDate today) {
        BigDecimal spread = grant.getCurrentValue().subtract(grant.getBaseValue());
        BigDecimal payout = spread.max(BigDecimal.ZERO);
        Map.Entry<LocalDate, BigDecimal> rate = this.rateOf(grant, rates, today);

        List<STARVestDTO> vests = StarVestingSchedule.tranches(grant.getCommencementDate(),
                        grant.getQuantity(), grant.getVestingYears()).stream()
                .map(tranche -> this.toVestDTO(grant, tranche, spread, payout, rate, today)).toList();

        BigDecimal totalAmountInHuf = vests.stream().map(STARVestDTO::getAmountInHuf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        TaxBreakdownDTO totalTax = vests.stream().map(STARVestDTO::getTax)
                .reduce(TaxBreakdownDTO.zero(), TaxBreakdownDTO::plus);

        return STARGrantDTO.builder()
                .id(grant.getId())
                .name(grant.getName())
                .currency(grant.getCurrency())
                .commencementDate(grant.getCommencementDate())
                .quantity(grant.getQuantity())
                .baseValue(grant.getBaseValue())
                .currentValue(grant.getCurrentValue())
                .spreadPerUnit(spread)
                .vestingYears(grant.getVestingYears())
                .note(grant.getNote())
                .vests(vests)
                .totalAmountInHuf(totalAmountInHuf)
                .totalTax(totalTax)
                .totalNetInHuf(this.netOf(vests, null))
                .vestedNetInHuf(this.netOf(vests, true))
                .upcomingNetInHuf(this.netOf(vests, false))
                .error((rate == null) ? "No MNB rate available for " + grant.getCurrency() : null)
                .build();
    }

    private STARVestDTO toVestDTO(STARGrant grant, StarVestingSchedule.Tranche tranche, BigDecimal spread,
                                  BigDecimal payout, Map.Entry<LocalDate, BigDecimal> rate, LocalDate today) {
        BigDecimal amount = payout.multiply(BigDecimal.valueOf(tranche.quantity()))
                .setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        BigDecimal amountInHuf = (rate == null) ? BigDecimal.ZERO
                : amount.multiply(rate.getValue()).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        TaxBreakdownDTO tax = this.taxService.calculateTax(amountInHuf);
        boolean vested = !tranche.vestDate().isAfter(today);

        return STARVestDTO.builder()
                .grantId(grant.getId())
                .name(grant.getName())
                .commencementDate(grant.getCommencementDate())
                .sequence(tranche.sequence())
                .vestDate(tranche.vestDate())
                .quantity(tranche.quantity())
                .cliff(tranche.cliff())
                .currency(grant.getCurrency())
                .spreadPerUnit(spread)
                .amount(amount)
                .rate((rate == null) ? null : rate.getValue())
                .rateDate((rate == null) ? null : rate.getKey())
                .amountInHuf(amountInHuf)
                .netInHuf(amountInHuf.subtract(tax.getTotal()))
                .vested(vested)
                .projected(!vested)
                .tax(tax)
                .build();
    }

    private STARGrantReportDTO report(List<STARGrantDTO> items) {
        BigDecimal totalAmountInHuf = items.stream().map(STARGrantDTO::getTotalAmountInHuf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        TaxBreakdownDTO totalTax = items.stream().map(STARGrantDTO::getTotalTax)
                .reduce(TaxBreakdownDTO.zero(), TaxBreakdownDTO::plus);
        BigDecimal totalNetInHuf = items.stream().map(STARGrantDTO::getTotalNetInHuf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return STARGrantReportDTO.builder().items(items).totalAmountInHuf(totalAmountInHuf)
                .totalTax(totalTax).totalNetInHuf(totalNetInHuf).build();
    }

    private BigDecimal netOf(List<STARVestDTO> vests, Boolean vested) {
        return vests.stream().filter(vest -> vested == null || vest.isVested() == vested)
                .map(STARVestDTO::getNetInHuf).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String name(String name) {
        return name.trim();
    }

    private String currency(String currency) {
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
