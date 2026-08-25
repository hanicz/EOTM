package eye.on.the.money.service.security;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.model.security.Security;
import eye.on.the.money.model.security.SecurityRate;
import eye.on.the.money.repository.security.SecurityRateRepository;
import eye.on.the.money.repository.security.SecurityRepository;
import eye.on.the.money.service.api.SecuritiesAPIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityRateService {

    private static final String ACT_360 = "_ACT_360";

    private static final Map<String, List<String>> TYPE_ALIASES = Map.of("FixMÁP", List.of("FMÁP"));

    private static final Duration REFRESH_INTERVAL = Duration.ofDays(7);

    private final SecurityRepository securityRepository;
    private final SecurityRateRepository securityRateRepository;
    private final SecuritiesAPIService securitiesAPIService;

    public void refreshIfStale() {
        log.trace("Enter refreshIfStale");
        LocalDateTime staleBefore = LocalDateTime.now().minus(SecurityRateService.REFRESH_INTERVAL);
        Optional<LocalDateTime> lastFetchedAt = this.securityRateRepository.findLastFetchedAt();
        if (lastFetchedAt.isPresent() && lastFetchedAt.get().isAfter(staleBefore)) {
            log.info("Security rates last refreshed at {}, next refresh due after {}",
                    lastFetchedAt.get(), lastFetchedAt.get().plus(SecurityRateService.REFRESH_INTERVAL));
            return;
        }
        this.refresh();
    }

    public void refresh() {
        log.trace("Enter refresh");
        Map<String, JsonNode> referenceByIsin = new HashMap<>();
        Map<String, String> isinByLookupKey = new HashMap<>();
        Set<String> ambiguousKeys = new HashSet<>();

        for (JsonNode reference : this.securitiesAPIService.getSecurities()) {
            String isin = this.text(reference, "isin");
            String type = this.text(reference, "securityType");
            String name = this.text(reference, "name");
            if (isin == null || type == null || name == null) continue;

            referenceByIsin.put(isin, reference);
            for (String key : this.lookupKeys(type, name)) {
                String existing = isinByLookupKey.putIfAbsent(key, isin);
                if (existing != null && !existing.equals(isin)) ambiguousKeys.add(key);
            }
        }
        ambiguousKeys.forEach(isinByLookupKey::remove);

        this.resolveIsins(isinByLookupKey);

        Map<String, JsonNode> interestByKey = new HashMap<>();
        for (JsonNode interest : this.securitiesAPIService.getActualInterests(LocalDate.now())) {
            String type = this.text(interest, "securityType");
            String name = this.text(interest, "name");
            if (type == null || name == null) continue;
            interestByKey.put(this.lookupKey(type, name), interest);
        }

        this.storeRates(referenceByIsin, interestByKey);
        log.trace("Exit refresh");
    }

    private void resolveIsins(Map<String, String> isinByLookupKey) {
        List<Security> unresolved = this.securityRepository.findByIsinIsNull();
        if (unresolved.isEmpty()) return;

        List<Security> resolved = new ArrayList<>();
        for (Security security : unresolved) {
            String isin = isinByLookupKey.get(security.getId());
            if (isin == null) {
                log.info("No reference match for security {}", security.getId());
                continue;
            }
            security.setIsin(isin);
            resolved.add(security);
            log.info("Resolved security {} to {}", security.getId(), isin);
        }
        this.securityRepository.saveAll(resolved);
    }

    private void storeRates(Map<String, JsonNode> referenceByIsin, Map<String, JsonNode> interestByKey) {
        List<SecurityRate> rates = new ArrayList<>();
        for (Security security : this.securityRepository.findByIsinIsNotNull()) {
            JsonNode reference = referenceByIsin.get(security.getIsin());
            if (reference == null) {
                log.warn("No reference data for security {} ({})", security.getId(), security.getIsin());
                continue;
            }
            String convention = this.text(reference, "interestConvention");
            JsonNode interest = interestByKey.get(
                    this.lookupKey(this.text(reference, "securityType"), this.text(reference, "name")));

            if (interest != null) {
                rates.add(this.toRate(security.getIsin(),
                        this.date(interest, "interestPeriodStart"),
                        this.date(interest, "interestPeriodEnd"),
                        this.date(interest, "paymentDate"),
                        interest.path("interest").asDouble(),
                        false,
                        convention));
            } else if (this.isZeroCoupon(reference)) {
                LocalDate maturityDate = this.date(reference, "maturityDate");
                rates.add(this.toRate(security.getIsin(),
                        this.date(reference, "issueDate"),
                        maturityDate,
                        maturityDate,
                        null,
                        true,
                        convention));
            } else {
                log.info("No current interest period for security {} ({})", security.getId(), security.getIsin());
            }
        }
        this.securityRateRepository.saveAll(rates.stream().filter(Objects::nonNull).toList());
    }

    private SecurityRate toRate(String isin, LocalDate periodStart, LocalDate periodEnd, LocalDate paymentDate,
                                Double rate, boolean zeroCoupon, String convention) {
        if (periodStart == null || periodEnd == null || paymentDate == null) {
            log.warn("Incomplete interest period for {}, skipping", isin);
            return null;
        }
        SecurityRate securityRate = this.securityRateRepository.findByIsinAndPeriodStart(isin, periodStart)
                .orElseGet(() -> SecurityRate.builder().isin(isin).periodStart(periodStart).build());
        securityRate.setPeriodEnd(periodEnd);
        securityRate.setPaymentDate(paymentDate);
        securityRate.setRate(rate);
        securityRate.setZeroCoupon(zeroCoupon);
        securityRate.setConvention(convention);
        securityRate.setFetchedAt(LocalDateTime.now());
        return securityRate;
    }

    public Map<String, SecurityRate> getNextPaymentByIsin(Collection<String> isins) {
        if (isins.isEmpty()) return Map.of();
        Map<String, SecurityRate> nextByIsin = new HashMap<>();
        this.securityRateRepository
                .findByIsinInAndPaymentDateGreaterThanEqualOrderByPaymentDateAsc(isins, LocalDate.now())
                .forEach(rate -> nextByIsin.putIfAbsent(rate.getIsin(), rate));
        return nextByIsin;
    }

    public double periodFraction(SecurityRate rate) {
        long days = ChronoUnit.DAYS.between(rate.getPeriodStart(), rate.getPeriodEnd());
        double basis = SecurityRateService.ACT_360.equals(rate.getConvention()) ? 360.0 : 365.0;
        return days / basis;
    }

    private List<String> lookupKeys(String type, String name) {
        List<String> keys = new ArrayList<>();
        keys.add(this.lookupKey(type, name));
        for (String alias : SecurityRateService.TYPE_ALIASES.getOrDefault(type, List.of())) {
            keys.add(this.lookupKey(alias, name));
        }
        int suffix = name.indexOf('_');
        if (suffix > 0) keys.add(this.lookupKey(type, name.substring(0, suffix)));
        return keys;
    }

    private String lookupKey(String type, String name) {
        return type + " " + name;
    }

    private boolean isZeroCoupon(JsonNode reference) {
        JsonNode coupon = reference.path("coupon");
        return coupon.isNumber() && coupon.asDouble() == 0.0;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private LocalDate date(JsonNode node, String field) {
        String value = this.text(node, field);
        return value == null ? null : LocalDate.parse(value);
    }
}
