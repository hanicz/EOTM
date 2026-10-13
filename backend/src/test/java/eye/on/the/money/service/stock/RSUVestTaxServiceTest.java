package eye.on.the.money.service.stock;

import eye.on.the.money.dto.in.RSUDTO;
import eye.on.the.money.dto.out.RSUTaxDTO;
import eye.on.the.money.dto.out.RSUVestTaxEventDTO;
import eye.on.the.money.dto.out.RSUVestTaxEventReportDTO;
import eye.on.the.money.dto.out.TaxReportDTO;
import eye.on.the.money.exception.TaxException;
import eye.on.the.money.model.User;
import eye.on.the.money.model.financial.TaxDetails;
import eye.on.the.money.model.stock.RSUGrant;
import eye.on.the.money.model.stock.RSUVestTaxDetails;
import eye.on.the.money.model.stock.VestingFrequency;
import eye.on.the.money.repository.stock.RSUGrantRepository;
import eye.on.the.money.repository.stock.RSUVestTaxDetailsRepository;
import eye.on.the.money.service.shared.TaxCalculator;
import eye.on.the.money.service.shared.TaxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class RSUVestTaxServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate GRANT_DATE = LocalDate.now().minusYears(2).minusMonths(1);
    private static final LocalDate FIRST_VEST = GRANT_DATE.plusYears(1);
    private static final LocalDate SECOND_VEST = GRANT_DATE.plusYears(2);
    private static final LocalDate CALCULATED_ON = LocalDate.of(2020, 1, 1);

    private final TaxCalculator taxCalculator = new TaxCalculator();

    @Mock
    private RSUGrantRepository rsuGrantRepository;

    @Mock
    private RSUVestTaxDetailsRepository rsuVestTaxDetailsRepository;

    @Mock
    private TaxService taxService;

    private RSUVestTaxService service() {
        return new RSUVestTaxService(this.rsuGrantRepository, this.rsuVestTaxDetailsRepository, this.taxService);
    }

    private RSUGrant grant() {
        return RSUGrant.builder()
                .id(7L)
                .shortName("ACME")
                .exchange("US")
                .currency("USD")
                .grantDate(GRANT_DATE)
                .quantity(100)
                .vestingYears(4)
                .vestingFrequency(VestingFrequency.ANNUAL)
                .user(User.builder().id(USER_ID).email("test@test.test").build())
                .build();
    }

    private RSUVestTaxDetails row(long id, RSUGrant grant, LocalDate vestDate, boolean paid) {
        return RSUVestTaxDetails.builder()
                .id(id)
                .grant(grant)
                .vestDate(vestDate)
                .sequence(1)
                .quantity(20)
                .price(new BigDecimal("50.00"))
                .priceDate(vestDate)
                .currency("USD")
                .taxDetails(TaxDetails.builder()
                        .rate(new BigDecimal("350"))
                        .rateDate(vestDate)
                        .amountInHuf(new BigDecimal("350000"))
                        .taxBase(new BigDecimal("311500"))
                        .szocho(new BigDecimal("40495"))
                        .szja(new BigDecimal("46725"))
                        .total(new BigDecimal("87220"))
                        .calculatedOn(CALCULATED_ON)
                        .paid(paid)
                        .build())
                .build();
    }

    private RSUTaxDTO valued(RSUDTO rsu) {
        BigDecimal amount = new BigDecimal("100.00").multiply(rsu.getQuantity());
        BigDecimal amountInHuf = amount.multiply(new BigDecimal("400"));
        return RSUTaxDTO.builder()
                .shortName(rsu.getShortName()).exchange(rsu.getExchange()).date(rsu.getDate())
                .quantity(rsu.getQuantity()).currency("USD")
                .price(new BigDecimal("100.00")).priceDate(rsu.getDate())
                .amount(amount)
                .rate(new BigDecimal("400")).rateDate(rsu.getDate())
                .amountInHuf(amountInHuf)
                .tax(this.taxCalculator.calculateTax(amountInHuf))
                .build();
    }

    private void stubValuation() {
        when(this.taxService.calculateTaxForRSUs(anyList())).thenAnswer(invocation -> {
            List<RSUDTO> rsus = invocation.getArgument(0);
            return TaxReportDTO.builder().items(rsus.stream().map(this::valued).toList()).build();
        });
    }

    private void stubSave() {
        when(this.rsuVestTaxDetailsRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void returnsAnEmptyReportWithoutGrants() {
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID)).thenReturn(List.of());
        when(this.rsuVestTaxDetailsRepository.findByUserId(USER_ID)).thenReturn(List.of());

        RSUVestTaxEventReportDTO report = this.service().getVestTaxEvents(USER_ID);

        assertTrue(report.getItems().isEmpty());
        verifyNoInteractions(this.taxService);
    }

    @Test
    void freezesOnlyTheTranchesThatHaveVested() {
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID)).thenReturn(List.of(this.grant()));
        when(this.rsuVestTaxDetailsRepository.findByUserId(USER_ID)).thenReturn(List.of());
        this.stubValuation();
        this.stubSave();

        RSUVestTaxEventReportDTO report = this.service().getVestTaxEvents(USER_ID);

        ArgumentCaptor<List<RSUDTO>> captor = ArgumentCaptor.captor();
        verify(this.taxService).calculateTaxForRSUs(captor.capture());
        assertEquals(List.of(FIRST_VEST, SECOND_VEST), captor.getValue().stream().map(RSUDTO::getDate).toList());
        assertEquals("USD", captor.getValue().getFirst().getCurrency());
        assertEquals(0, new BigDecimal("25").compareTo(captor.getValue().getFirst().getQuantity()));

        assertEquals(2, report.getItems().size());
        RSUVestTaxEventDTO latest = report.getItems().getFirst();
        assertEquals(SECOND_VEST, latest.getVestDate());
        assertEquals(2, latest.getSequence());
        assertEquals(25, latest.getQuantity());
        assertEquals(0, new BigDecimal("2500.00").compareTo(latest.getAmount()));
        assertEquals(0, new BigDecimal("1000000").compareTo(latest.getAmountInHuf()));
        assertEquals(LocalDate.now(), latest.getCalculatedOn());
        assertFalse(latest.isPaid());
        assertNull(report.getError());
        assertEquals(0, new BigDecimal("2000000").compareTo(report.getTotalAmountInHuf()));
    }

    @Test
    void doesNotRevalueVestsThatAreAlreadyFrozen() {
        RSUGrant grant = this.grant();
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID)).thenReturn(List.of(grant));
        when(this.rsuVestTaxDetailsRepository.findByUserId(USER_ID)).thenReturn(List.of(
                this.row(1L, grant, SECOND_VEST, false), this.row(2L, grant, FIRST_VEST, true)));

        RSUVestTaxEventReportDTO report = this.service().getVestTaxEvents(USER_ID);

        verifyNoInteractions(this.taxService);
        assertEquals(2, report.getItems().size());
        assertEquals(CALCULATED_ON, report.getItems().getFirst().getCalculatedOn());
        assertTrue(report.getItems().getLast().isPaid());
        assertEquals(0, new BigDecimal("174440").compareTo(report.getTotalTax().getTotal()));
    }

    @Test
    void keepsTheFrozenVestsAndReportsAnErrorWhenValuationFails() {
        RSUGrant grant = this.grant();
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID)).thenReturn(List.of(grant));
        when(this.rsuVestTaxDetailsRepository.findByUserId(USER_ID))
                .thenReturn(List.of(this.row(1L, grant, FIRST_VEST, false)));
        when(this.taxService.calculateTaxForRSUs(anyList())).thenThrow(new TaxException("No closing price"));

        RSUVestTaxEventReportDTO report = this.service().getVestTaxEvents(USER_ID);

        assertEquals(1, report.getItems().size());
        assertNotNull(report.getError());
        assertTrue(report.getError().contains("ACME"));
        verify(this.rsuVestTaxDetailsRepository, never()).saveAll(any());
    }

    @Test
    void dropsUnpaidVestsThatAreNoLongerOnTheScheduleButKeepsPaidOnes() {
        RSUGrant grant = this.grant();
        RSUVestTaxDetails unpaidOrphan = this.row(3L, grant, FIRST_VEST.minusDays(3), false);
        RSUVestTaxDetails paidOrphan = this.row(4L, grant, FIRST_VEST.minusDays(5), true);
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID)).thenReturn(List.of(grant));
        when(this.rsuVestTaxDetailsRepository.findByUserId(USER_ID)).thenReturn(List.of(
                this.row(1L, grant, SECOND_VEST, false), this.row(2L, grant, FIRST_VEST, false),
                unpaidOrphan, paidOrphan));

        RSUVestTaxEventReportDTO report = this.service().getVestTaxEvents(USER_ID);

        verify(this.rsuVestTaxDetailsRepository).deleteAll(List.of(unpaidOrphan));
        assertEquals(List.of(1L, 2L, 4L), report.getItems().stream().map(RSUVestTaxEventDTO::getId).toList());
        verifyNoInteractions(this.taxService);
    }

    @Test
    void marksTheTaxAsPaid() {
        RSUVestTaxDetails row = this.row(1L, this.grant(), FIRST_VEST, false);
        when(this.rsuVestTaxDetailsRepository.findByUserIdAndIdIn(USER_ID, List.of(1L))).thenReturn(List.of(row));

        this.service().setTaxPaid(USER_ID, List.of(1L), true);

        assertTrue(row.getTaxDetails().isPaid());
        verify(this.rsuVestTaxDetailsRepository).saveAll(List.of(row));
    }

    @Test
    void ignoresIdsOfAnotherUserWhenMarkingAsPaid() {
        when(this.rsuVestTaxDetailsRepository.findByUserIdAndIdIn(USER_ID, List.of(9L))).thenReturn(List.of());

        this.service().setTaxPaid(USER_ID, List.of(9L), true);

        verify(this.rsuVestTaxDetailsRepository, never()).saveAll(any());
    }

    @Test
    void recalculatesFromTheCurrentScheduleAndKeepsThePaidFlag() {
        RSUVestTaxDetails row = this.row(1L, this.grant(), FIRST_VEST, true);
        when(this.rsuVestTaxDetailsRepository.findByUserIdAndIdIn(USER_ID, List.of(1L))).thenReturn(List.of(row));
        this.stubValuation();

        this.service().recalculate(USER_ID, List.of(1L));

        assertEquals(25, row.getQuantity());
        assertEquals(0, new BigDecimal("100.00").compareTo(row.getPrice()));
        assertEquals(0, new BigDecimal("1000000").compareTo(row.getTaxDetails().getAmountInHuf()));
        assertEquals(LocalDate.now(), row.getTaxDetails().getCalculatedOn());
        assertTrue(row.getTaxDetails().isPaid());
        verify(this.rsuVestTaxDetailsRepository).saveAll(List.of(row));
    }

    @Test
    void skipsRecalculatingAVestThatIsNoLongerOnTheSchedule() {
        RSUVestTaxDetails row = this.row(1L, this.grant(), FIRST_VEST.minusDays(3), false);
        when(this.rsuVestTaxDetailsRepository.findByUserIdAndIdIn(USER_ID, List.of(1L))).thenReturn(List.of(row));

        this.service().recalculate(USER_ID, List.of(1L));

        verifyNoInteractions(this.taxService);
        verify(this.rsuVestTaxDetailsRepository, never()).saveAll(any());
    }

    @Test
    void writesTheReportAsCsv() {
        RSUGrant grant = this.grant();
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID)).thenReturn(List.of(grant));
        when(this.rsuVestTaxDetailsRepository.findByUserId(USER_ID)).thenReturn(List.of(
                this.row(1L, grant, SECOND_VEST, false), this.row(2L, grant, FIRST_VEST, false)));
        StringWriter writer = new StringWriter();

        this.service().getCSV(USER_ID, writer);

        String csv = writer.toString();
        assertTrue(csv.contains("Vest Date"));
        assertTrue(csv.contains(SECOND_VEST.toString()));
        assertTrue(csv.contains("87220"));
    }
}
