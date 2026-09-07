package eye.on.the.money.service.stock;

import eye.on.the.money.dto.in.RSUDTO;
import eye.on.the.money.dto.in.RSUGrantEditDTO;
import eye.on.the.money.dto.out.RSUGrantDTO;
import eye.on.the.money.dto.out.RSUGrantReportDTO;
import eye.on.the.money.dto.out.RSUTaxDTO;
import eye.on.the.money.dto.out.RSUVestDTO;
import eye.on.the.money.dto.out.TaxBreakdownDTO;
import eye.on.the.money.dto.out.TaxReportDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.exception.TaxException;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.RSUGrant;
import eye.on.the.money.model.stock.VestingFrequency;
import eye.on.the.money.repository.stock.RSUGrantRepository;
import eye.on.the.money.service.shared.TaxService;
import eye.on.the.money.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.context.ActiveProfiles;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RSUGrantServiceTest {

    private static final Long USER_ID = 42L;
    private static final String TICKER = "ACME";
    private static final LocalDate LONG_AGO = LocalDate.now().minusYears(8);

    @Mock
    private RSUGrantRepository rsuGrantRepository;

    @Mock
    private TaxService taxService;

    @Mock
    private UserService userService;

    @InjectMocks
    private RSUGrantService rsuGrantService;

    private RSUGrant grant(Long id, LocalDate grantDate, int quantity, int years, VestingFrequency frequency) {
        return RSUGrant.builder().id(id).shortName(TICKER).exchange("US").grantDate(grantDate)
                .quantity(quantity).vestingYears(years).vestingFrequency(frequency)
                .user(User.builder().id(USER_ID).build()).build();
    }

    private void answerWithFlatValuation(BigDecimal amountInHuf) {
        when(this.taxService.calculateTaxForRSUs(anyList())).thenAnswer(invocation -> {
            List<RSUDTO> requests = invocation.getArgument(0);
            List<RSUTaxDTO> items = requests.stream().map(request -> RSUTaxDTO.builder()
                    .shortName(request.getShortName()).exchange(request.getExchange())
                    .date(request.getDate()).quantity(request.getQuantity()).currency("USD")
                    .price(new BigDecimal("100")).priceDate(request.getDate())
                    .amount(new BigDecimal("100").multiply(BigDecimal.valueOf(request.getQuantity())))
                    .rate(new BigDecimal("350")).rateDate(request.getDate())
                    .amountInHuf(amountInHuf)
                    .tax(TaxBreakdownDTO.builder().amount(amountInHuf).taxBase(amountInHuf)
                            .szocho(new BigDecimal("100")).szja(new BigDecimal("50"))
                            .total(new BigDecimal("150")).build())
                    .build()).toList();
            return TaxReportDTO.builder().items(items).totalAmountInHuf(BigDecimal.ZERO)
                    .totalTax(TaxBreakdownDTO.zero()).build();
        });
    }

    @Test
    void getGrants_returnsAnEmptyReportWhenThereAreNoGrants() {
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID)).thenReturn(List.of());

        RSUGrantReportDTO report = this.rsuGrantService.getGrants(USER_ID);

        assertEquals(List.of(), report.getItems());
        assertEquals(BigDecimal.ZERO, report.getTotalAmountInHuf());
        verify(this.taxService, times(0)).calculateTaxForRSUs(anyList());
    }

    @Test
    void getGrants_splitsAnAnnualGrantIntoFourEqualYearlyVests() {
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID))
                .thenReturn(List.of(this.grant(1L, LONG_AGO, 400, 4, VestingFrequency.ANNUAL)));
        this.answerWithFlatValuation(new BigDecimal("1000"));

        List<RSUVestDTO> vests = this.rsuGrantService.getGrants(USER_ID).getItems().getFirst().getVests();

        assertEquals(4, vests.size());
        assertEquals(List.of(100, 100, 100, 100), vests.stream().map(RSUVestDTO::getQuantity).toList());
        assertEquals(List.of(1, 2, 3, 4), vests.stream().map(RSUVestDTO::getSequence).toList());
        assertEquals(List.of(LONG_AGO.plusYears(1), LONG_AGO.plusYears(2), LONG_AGO.plusYears(3),
                LONG_AGO.plusYears(4)), vests.stream().map(RSUVestDTO::getVestDate).toList());
    }

    @Test
    void getGrants_spreadsTheLeftoverSharesAcrossTheSchedule() {
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID))
                .thenReturn(List.of(this.grant(1L, LONG_AGO, 118, 4, VestingFrequency.ANNUAL)));
        this.answerWithFlatValuation(new BigDecimal("1000"));

        List<RSUVestDTO> vests = this.rsuGrantService.getGrants(USER_ID).getItems().getFirst().getVests();

        assertEquals(List.of(29, 30, 29, 30), vests.stream().map(RSUVestDTO::getQuantity).toList());
    }

    @Test
    void getGrants_leavesASingleLeftoverShareOnTheLastVest() {
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID))
                .thenReturn(List.of(this.grant(1L, LONG_AGO, 401, 4, VestingFrequency.ANNUAL)));
        this.answerWithFlatValuation(new BigDecimal("1000"));

        List<RSUVestDTO> vests = this.rsuGrantService.getGrants(USER_ID).getItems().getFirst().getVests();

        assertEquals(List.of(100, 100, 100, 101), vests.stream().map(RSUVestDTO::getQuantity).toList());
    }

    @Test
    void getGrants_neverLosesOrInventsAShareWhateverTheSchedule() {
        for (int quantity : new int[]{1, 7, 100, 101, 118, 399, 401, 1000}) {
            for (VestingFrequency frequency : VestingFrequency.values()) {
                for (int years = 1; years <= RSUGrant.MAX_VESTING_YEARS; years++) {
                    when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID))
                            .thenReturn(List.of(this.grant(1L, LONG_AGO, quantity, years, frequency)));
                    this.answerWithFlatValuation(new BigDecimal("1000"));

                    List<RSUVestDTO> vests = this.rsuGrantService.getGrants(USER_ID)
                            .getItems().getFirst().getVests();
                    int total = vests.stream().mapToInt(RSUVestDTO::getQuantity).sum();
                    int smallest = vests.stream().mapToInt(RSUVestDTO::getQuantity).min().orElseThrow();
                    int largest = vests.stream().mapToInt(RSUVestDTO::getQuantity).max().orElseThrow();

                    assertEquals(quantity, total, quantity + " over " + years + "y " + frequency);
                    assertTrue(largest - smallest <= 1, quantity + " over " + years + "y " + frequency
                            + " spread " + smallest + ".." + largest);
                }
            }
        }
    }

    @Test
    void getGrants_spreadsAQuarterlyGrantOverThreeMonthSteps() {
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID))
                .thenReturn(List.of(this.grant(1L, LONG_AGO, 80, 2, VestingFrequency.QUARTERLY)));
        this.answerWithFlatValuation(new BigDecimal("1000"));

        List<RSUVestDTO> vests = this.rsuGrantService.getGrants(USER_ID).getItems().getFirst().getVests();

        assertEquals(8, vests.size());
        assertEquals(LONG_AGO.plusMonths(3), vests.getFirst().getVestDate());
        assertEquals(LONG_AGO.plusMonths(24), vests.getLast().getVestDate());
        assertTrue(vests.stream().allMatch(vest -> vest.getQuantity() == 10));
    }

    @Test
    void getGrants_valuesFutureVestsAsOfTodayAndFlagsThemProjected() {
        LocalDate grantDate = LocalDate.now().minusMonths(6);
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID))
                .thenReturn(List.of(this.grant(1L, grantDate, 400, 4, VestingFrequency.ANNUAL)));
        this.answerWithFlatValuation(new BigDecimal("1000"));

        List<RSUVestDTO> vests = this.rsuGrantService.getGrants(USER_ID).getItems().getFirst().getVests();

        assertTrue(vests.stream().allMatch(RSUVestDTO::isProjected));
        assertTrue(vests.stream().noneMatch(RSUVestDTO::isVested));

        ArgumentCaptor<List<RSUDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(this.taxService).calculateTaxForRSUs(captor.capture());
        assertTrue(captor.getValue().stream().allMatch(request -> LocalDate.now().equals(request.getDate())));
    }

    @Test
    void getGrants_valuesPastVestsOnTheirOwnVestDate() {
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID))
                .thenReturn(List.of(this.grant(1L, LONG_AGO, 400, 4, VestingFrequency.ANNUAL)));
        this.answerWithFlatValuation(new BigDecimal("1000"));

        List<RSUVestDTO> vests = this.rsuGrantService.getGrants(USER_ID).getItems().getFirst().getVests();

        assertTrue(vests.stream().allMatch(RSUVestDTO::isVested));
        assertTrue(vests.stream().noneMatch(RSUVestDTO::isProjected));

        ArgumentCaptor<List<RSUDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(this.taxService).calculateTaxForRSUs(captor.capture());
        assertEquals(LONG_AGO.plusYears(1), captor.getValue().getFirst().getDate());
    }

    @Test
    void getGrants_marksEveryVestOfAFullyVestedGrantAsVested() {
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID))
                .thenReturn(List.of(this.grant(1L, LONG_AGO, 400, 4, VestingFrequency.ANNUAL)));
        this.answerWithFlatValuation(new BigDecimal("1000"));

        List<RSUVestDTO> vests = this.rsuGrantService.getGrants(USER_ID).getItems().getFirst().getVests();

        assertFalse(vests.isEmpty());
        assertTrue(vests.stream().allMatch(RSUVestDTO::isVested));
    }

    @Test
    void getGrants_returnsTheVestsInSequenceOrderSoTheFirstUnvestedOneIsTheNext() {
        LocalDate grantDate = LocalDate.now().minusYears(2).minusMonths(1);
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID))
                .thenReturn(List.of(this.grant(1L, grantDate, 400, 4, VestingFrequency.ANNUAL)));
        this.answerWithFlatValuation(new BigDecimal("1000"));

        List<RSUVestDTO> vests = this.rsuGrantService.getGrants(USER_ID).getItems().getFirst().getVests();

        assertEquals(List.of(1, 2, 3, 4), vests.stream().map(RSUVestDTO::getSequence).toList());
        assertEquals(List.of(true, true, false, false), vests.stream().map(RSUVestDTO::isVested).toList());

        RSUVestDTO next = vests.stream().filter(vest -> !vest.isVested()).findFirst().orElseThrow();
        assertEquals(3, next.getSequence());
        assertEquals(grantDate.plusYears(3), next.getVestDate());
    }

    @Test
    void getGrants_subtractsTheTaxFromTheGrossToGetTheNet() {
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID))
                .thenReturn(List.of(this.grant(1L, LONG_AGO, 400, 4, VestingFrequency.ANNUAL)));
        this.answerWithFlatValuation(new BigDecimal("1000"));

        RSUGrantDTO priced = this.rsuGrantService.getGrants(USER_ID).getItems().getFirst();

        assertEquals(new BigDecimal("850"), priced.getVests().getFirst().getNetInHuf());
        assertEquals(new BigDecimal("4000"), priced.getTotalAmountInHuf());
        assertEquals(new BigDecimal("600"), priced.getTotalTax().getTotal());
        assertEquals(new BigDecimal("3400"), priced.getTotalNetInHuf());
        assertEquals(new BigDecimal("3400"), priced.getVestedNetInHuf());
        assertEquals(BigDecimal.ZERO, priced.getUpcomingNetInHuf());
    }

    @Test
    void getGrants_addsUpEveryGrantIntoTheReportTotals() {
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID)).thenReturn(List.of(
                this.grant(1L, LONG_AGO, 400, 4, VestingFrequency.ANNUAL),
                this.grant(2L, LONG_AGO.plusYears(1), 400, 4, VestingFrequency.ANNUAL)));
        this.answerWithFlatValuation(new BigDecimal("1000"));

        RSUGrantReportDTO report = this.rsuGrantService.getGrants(USER_ID);

        assertEquals(2, report.getItems().size());
        assertEquals(new BigDecimal("8000"), report.getTotalAmountInHuf());
        assertEquals(new BigDecimal("1200"), report.getTotalTax().getTotal());
        assertEquals(new BigDecimal("6800"), report.getTotalNetInHuf());
        verify(this.taxService, times(1)).calculateTaxForRSUs(anyList());
    }

    @Test
    void getGrants_pricesGrantsOneByOneWhenTheBatchedCallFails() {
        RSUGrant good = this.grant(1L, LONG_AGO, 400, 4, VestingFrequency.ANNUAL);
        RSUGrant bad = RSUGrant.builder().id(2L).shortName("NOPE").exchange("US").grantDate(LONG_AGO)
                .quantity(400).vestingYears(4).vestingFrequency(VestingFrequency.ANNUAL).build();
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID))
                .thenReturn(List.of(good, bad));

        when(this.taxService.calculateTaxForRSUs(anyList())).thenAnswer(invocation -> {
            List<RSUDTO> requests = invocation.getArgument(0);
            if (requests.stream().anyMatch(request -> "NOPE".equals(request.getShortName()))) {
                throw new TaxException("No closing price available for NOPE.US");
            }
            return TaxReportDTO.builder().items(requests.stream().map(request -> RSUTaxDTO.builder()
                    .amountInHuf(new BigDecimal("1000")).amount(new BigDecimal("10"))
                    .tax(TaxBreakdownDTO.zero()).build()).toList())
                    .totalAmountInHuf(BigDecimal.ZERO).totalTax(TaxBreakdownDTO.zero()).build();
        });

        List<RSUGrantDTO> items = this.rsuGrantService.getGrants(USER_ID).getItems();

        assertNull(items.getFirst().getError());
        assertEquals(new BigDecimal("4000"), items.getFirst().getTotalAmountInHuf());
        assertNotNull(items.get(1).getError());
        assertEquals(4, items.get(1).getVests().size());
        assertEquals(BigDecimal.ZERO, items.get(1).getTotalAmountInHuf());
        verify(this.taxService, times(3)).calculateTaxForRSUs(anyList());
    }

    @Test
    void getGrants_alsoIsolatesAGrantTheQuoteApiRefuses() {
        RSUGrant good = this.grant(1L, LONG_AGO, 400, 4, VestingFrequency.ANNUAL);
        RSUGrant delisted = RSUGrant.builder().id(2L).shortName("GONE").exchange("US").grantDate(LONG_AGO)
                .quantity(400).vestingYears(4).vestingFrequency(VestingFrequency.ANNUAL).build();
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID))
                .thenReturn(List.of(good, delisted));

        when(this.taxService.calculateTaxForRSUs(anyList())).thenAnswer(invocation -> {
            List<RSUDTO> requests = invocation.getArgument(0);
            if (requests.stream().anyMatch(request -> "GONE".equals(request.getShortName()))) {
                throw new APIException("Unable to make GET call404 NOT_FOUND");
            }
            return TaxReportDTO.builder().items(requests.stream().map(request -> RSUTaxDTO.builder()
                    .amountInHuf(new BigDecimal("1000")).amount(new BigDecimal("10"))
                    .tax(TaxBreakdownDTO.zero()).build()).toList())
                    .totalAmountInHuf(BigDecimal.ZERO).totalTax(TaxBreakdownDTO.zero()).build();
        });

        List<RSUGrantDTO> items = this.rsuGrantService.getGrants(USER_ID).getItems();

        assertNull(items.getFirst().getError());
        assertNotNull(items.get(1).getError());
        assertTrue(items.get(1).getError().contains("GONE.US"));
    }

    @Test
    void createGrant_refusesATickerTheQuoteApiCannotPrice() {
        when(this.userService.getReference(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
        when(this.rsuGrantRepository.saveAndFlush(any(RSUGrant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(this.taxService.calculateTaxForRSUs(anyList()))
                .thenThrow(new APIException("Unable to make GET call404 NOT_FOUND"));

        assertThrows(APIException.class, () -> this.rsuGrantService.createGrant(USER_ID, new RSUGrantEditDTO(
                "GONE", "US", null, LONG_AGO, 400, 4, VestingFrequency.ANNUAL, null)));
    }

    @Test
    void createGrant_normalizesTheTickerAndDefaultsTheExchange() {
        when(this.userService.getReference(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
        when(this.rsuGrantRepository.saveAndFlush(any(RSUGrant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        this.answerWithFlatValuation(new BigDecimal("1000"));

        RSUGrantDTO created = this.rsuGrantService.createGrant(USER_ID, new RSUGrantEditDTO(
                " acme ", "  ", " usd ", LONG_AGO, 400, 4, VestingFrequency.ANNUAL, "  "));

        assertEquals(TICKER, created.getShortName());
        assertEquals("US", created.getExchange());
        assertEquals("USD", created.getCurrency());
        assertNull(created.getNote());
    }

    @Test
    void updateGrant_rejectsAGrantThatIsNotYours() {
        when(this.rsuGrantRepository.findByIdAndUserId(9L, USER_ID)).thenReturn(java.util.Optional.empty());

        assertThrows(java.util.NoSuchElementException.class, () -> this.rsuGrantService.updateGrant(USER_ID, 9L,
                new RSUGrantEditDTO(TICKER, "US", null, LONG_AGO, 400, 4, VestingFrequency.ANNUAL, null)));
    }

    @Test
    void updateGrant_overwritesTheStoredGrant() {
        RSUGrant existing = this.grant(5L, LONG_AGO, 400, 4, VestingFrequency.ANNUAL);
        when(this.rsuGrantRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(java.util.Optional.of(existing));
        when(this.rsuGrantRepository.saveAndFlush(existing)).thenReturn(existing);
        this.answerWithFlatValuation(new BigDecimal("1000"));

        RSUGrantDTO updated = this.rsuGrantService.updateGrant(USER_ID, 5L, new RSUGrantEditDTO(
                "widget", "LSE", null, LONG_AGO.plusDays(1), 80, 2, VestingFrequency.QUARTERLY, "new plan"));

        assertEquals("WIDGET", updated.getShortName());
        assertEquals("LSE", updated.getExchange());
        assertEquals(8, updated.getVests().size());
        assertEquals("new plan", updated.getNote());
    }

    @Test
    void deleteGrantsByIds_scopesTheDeleteToTheUser() {
        this.rsuGrantService.deleteGrantsByIds(USER_ID, List.of(1L, 2L));

        verify(this.rsuGrantRepository).deleteByUserIdAndIdIn(eq(USER_ID), eq(List.of(1L, 2L)));
    }

    @Test
    void getCSV_writesOneRowPerVestUnderASingleHeader() {
        when(this.rsuGrantRepository.findByUserIdOrderByGrantDateDescIdAsc(USER_ID))
                .thenReturn(List.of(this.grant(1L, LONG_AGO, 400, 4, VestingFrequency.ANNUAL)));
        this.answerWithFlatValuation(new BigDecimal("1000"));
        StringWriter writer = new StringWriter();

        this.rsuGrantService.getCSV(USER_ID, writer);

        String[] lines = writer.toString().split("\\r?\\n");
        assertEquals(5, lines.length);
        assertTrue(lines[0].startsWith("Ticker,Exchange,Grant Date,Vest,Vest Date"));
        assertTrue(lines[1].startsWith(TICKER + ",US,"));
        assertFalse(lines[1].contains("Ticker"));
    }
}
