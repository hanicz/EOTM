package eye.on.the.money.service.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.model.security.Security;
import eye.on.the.money.model.security.SecurityRate;
import eye.on.the.money.repository.security.SecurityRateRepository;
import eye.on.the.money.repository.security.SecurityRepository;
import eye.on.the.money.service.api.SecuritiesAPIService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class SecurityRateServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private SecurityRateRepository securityRateRepository;
    @Mock
    private SecuritiesAPIService securitiesAPIService;

    @InjectMocks
    private SecurityRateService securityRateService;

    @Test
    void refresh_resolvesIsinOnCanonicalKey() {
        this.givenReferenceSecurities("""
                [{"isin":"HU0000406954","securityType":"PMÁP","name":"2032/J","interestConvention":"_ACT_ACT"}]""");
        this.givenActualInterests("[]");
        this.givenUnresolved("PMÁP 2032/J");

        this.securityRateService.refresh();

        assertEquals("HU0000406954", this.captureResolved().getIsin());
    }

    @Test
    void refresh_resolvesIsinOnTypeAlias() {
        this.givenReferenceSecurities("""
                [{"isin":"HU0000407655","securityType":"FixMÁP","name":"2029/Q1","interestConvention":"_ACT_ACT"}]""");
        this.givenActualInterests("[]");
        this.givenUnresolved("FMÁP 2029/Q1");

        this.securityRateService.refresh();

        assertEquals("HU0000407655", this.captureResolved().getIsin());
    }

    @Test
    void refresh_resolvesIsinOnStrippedNameSuffix() {
        this.givenReferenceSecurities("""
                [{"isin":"HU0000407218","securityType":"EMÁP","name":"2028/U_EUR","interestConvention":"_ACT_360"}]""");
        this.givenActualInterests("[]");
        this.givenUnresolved("EMÁP 2028/U");

        this.securityRateService.refresh();

        assertEquals("HU0000407218", this.captureResolved().getIsin());
    }

    @Test
    void refresh_resolvesIsinWhenTypeContainsSpace() {
        this.givenReferenceSecurities("""
                [{"isin":"HU0000405162","securityType":"MÁP Plusz","name":"N2026/35","interestConvention":"_ACT_ACT"}]""");
        this.givenActualInterests("[]");
        this.givenUnresolved("MÁP Plusz N2026/35");

        this.securityRateService.refresh();

        assertEquals("HU0000405162", this.captureResolved().getIsin());
    }

    @Test
    void refresh_skipsAmbiguousLookupKey() {
        this.givenReferenceSecurities("""
                [{"isin":"HU1111111111","securityType":"EMÁP","name":"2028/U_EUR","interestConvention":"_ACT_360"},
                 {"isin":"HU2222222222","securityType":"EMÁP","name":"2028/U_HUF","interestConvention":"_ACT_360"}]""");
        this.givenActualInterests("[]");
        this.givenUnresolved("EMÁP 2028/U");

        this.securityRateService.refresh();

        ArgumentCaptor<List<Security>> captor = ArgumentCaptor.forClass(List.class);
        verify(this.securityRepository).saveAll(captor.capture());
        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    void refresh_storesCouponPeriodFromActualInterests() {
        this.givenReferenceSecurities("""
                [{"isin":"HU0000407168","securityType":"BMÁP","name":"2028/P","interestConvention":"_ACT_360"}]""");
        this.givenActualInterests("""
                [{"securityType":"BMÁP","name":"2028/P","interest":7.38,"interestPeriodStart":"2026-05-25",
                  "interestPeriodEnd":"2026-08-25","paymentDate":"2026-08-25"}]""");
        this.givenResolved("BMÁP 2028/P", "HU0000407168");

        this.securityRateService.refresh();

        SecurityRate stored = this.captureStoredRate();
        assertEquals(7.38, stored.getRate());
        assertFalse(stored.getZeroCoupon());
        assertEquals(LocalDate.of(2026, 8, 25), stored.getPaymentDate());
        assertEquals("_ACT_360", stored.getConvention());
    }

    @Test
    void refresh_storesZeroCouponPeriodWhenNoInterestRow() {
        this.givenReferenceSecurities("""
                [{"isin":"HU0000525225","securityType":"DKJ","name":"D261028","interestConvention":"_ACT_360",
                  "coupon":0,"issueDate":"2025-10-29","maturityDate":"2026-10-28"}]""");
        this.givenActualInterests("[]");
        this.givenResolved("DKJ D261028", "HU0000525225");

        this.securityRateService.refresh();

        SecurityRate stored = this.captureStoredRate();
        assertTrue(stored.getZeroCoupon());
        assertNull(stored.getRate());
        assertEquals(LocalDate.of(2026, 10, 28), stored.getPaymentDate());
        assertEquals(LocalDate.of(2026, 10, 28), stored.getPeriodEnd());
    }

    @Test
    void refreshIfStale_skipsWhenSnapshotIsWithinTheWeek() {
        when(this.securityRateRepository.findLastFetchedAt())
                .thenReturn(Optional.of(LocalDateTime.now().minusDays(6)));

        this.securityRateService.refreshIfStale();

        verifyNoInteractions(this.securitiesAPIService);
    }

    @Test
    void refreshIfStale_refreshesWhenSnapshotIsOlderThanAWeek() {
        when(this.securityRateRepository.findLastFetchedAt())
                .thenReturn(Optional.of(LocalDateTime.now().minusDays(8)));
        this.givenReferenceSecurities("[]");
        this.givenActualInterests("[]");
        when(this.securityRepository.findByIsinIsNull()).thenReturn(List.of());
        when(this.securityRepository.findByIsinIsNotNull()).thenReturn(List.of());

        this.securityRateService.refreshIfStale();

        verify(this.securitiesAPIService).getSecurities();
    }

    @Test
    void refreshIfStale_refreshesWhenSnapshotIsEmpty() {
        when(this.securityRateRepository.findLastFetchedAt()).thenReturn(Optional.empty());
        this.givenReferenceSecurities("[]");
        this.givenActualInterests("[]");
        when(this.securityRepository.findByIsinIsNull()).thenReturn(List.of());
        when(this.securityRepository.findByIsinIsNotNull()).thenReturn(List.of());

        this.securityRateService.refreshIfStale();

        verify(this.securitiesAPIService).getSecurities();
    }

    @Test
    void periodFraction_usesThreeHundredSixtyBasisForActThreeSixty() {
        SecurityRate rate = SecurityRate.builder()
                .periodStart(LocalDate.of(2026, 5, 25))
                .periodEnd(LocalDate.of(2026, 8, 25))
                .convention("_ACT_360")
                .build();

        assertEquals(92.0 / 360.0, this.securityRateService.periodFraction(rate), 1e-9);
    }

    @Test
    void periodFraction_usesThreeHundredSixtyFiveBasisOtherwise() {
        SecurityRate rate = SecurityRate.builder()
                .periodStart(LocalDate.of(2026, 4, 22))
                .periodEnd(LocalDate.of(2027, 4, 22))
                .convention("_ACT_ACT")
                .build();

        assertEquals(1.0, this.securityRateService.periodFraction(rate), 1e-9);
    }

    @Test
    void getNextPaymentByIsin_keepsEarliestPaymentPerIsin() {
        SecurityRate earlier = SecurityRate.builder().isin("HU1").paymentDate(LocalDate.of(2026, 9, 1)).build();
        SecurityRate later = SecurityRate.builder().isin("HU1").paymentDate(LocalDate.of(2027, 9, 1)).build();
        when(this.securityRateRepository
                .findByIsinInAndPaymentDateGreaterThanEqualOrderByPaymentDateAsc(anyList(), any(LocalDate.class)))
                .thenReturn(List.of(earlier, later));

        Map<String, SecurityRate> result = this.securityRateService.getNextPaymentByIsin(List.of("HU1"));

        assertEquals(LocalDate.of(2026, 9, 1), result.get("HU1").getPaymentDate());
    }

    @Test
    void getNextPaymentByIsin_returnsEmptyWithoutQueryingWhenNoIsins() {
        assertTrue(this.securityRateService.getNextPaymentByIsin(List.of()).isEmpty());
        verifyNoInteractions(this.securityRateRepository);
    }

    private void givenReferenceSecurities(String json) {
        when(this.securitiesAPIService.getSecurities()).thenReturn(this.parse(json));
    }

    private void givenActualInterests(String json) {
        when(this.securitiesAPIService.getActualInterests(any(LocalDate.class))).thenReturn(this.parse(json));
    }

    private void givenUnresolved(String securityId) {
        when(this.securityRepository.findByIsinIsNull())
                .thenReturn(List.of(Security.builder().id(securityId).name(securityId).build()));
        when(this.securityRepository.findByIsinIsNotNull()).thenReturn(List.of());
    }

    private void givenResolved(String securityId, String isin) {
        when(this.securityRepository.findByIsinIsNull()).thenReturn(List.of());
        when(this.securityRepository.findByIsinIsNotNull())
                .thenReturn(List.of(Security.builder().id(securityId).name(securityId).isin(isin).build()));
        when(this.securityRateRepository.findByIsinAndPeriodStart(any(), any())).thenReturn(Optional.empty());
    }

    private Security captureResolved() {
        ArgumentCaptor<List<Security>> captor = ArgumentCaptor.forClass(List.class);
        verify(this.securityRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        return captor.getValue().getFirst();
    }

    private SecurityRate captureStoredRate() {
        ArgumentCaptor<List<SecurityRate>> captor = ArgumentCaptor.forClass(List.class);
        verify(this.securityRateRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        return captor.getValue().getFirst();
    }

    private JsonNode parse(String json) {
        try {
            return SecurityRateServiceTest.MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
