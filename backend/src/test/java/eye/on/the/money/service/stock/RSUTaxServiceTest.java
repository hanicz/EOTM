package eye.on.the.money.service.stock;

import eye.on.the.money.dto.in.RSUDTO;
import eye.on.the.money.dto.out.RSUTaxDTO;
import eye.on.the.money.dto.out.RSUTaxEventDTO;
import eye.on.the.money.dto.out.RSUTaxEventReportDTO;
import eye.on.the.money.dto.out.TaxBreakdownDTO;
import eye.on.the.money.dto.out.TaxReportDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.financial.TaxDetails;
import eye.on.the.money.model.stock.Investment;
import eye.on.the.money.model.stock.RSUTaxDetails;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.repository.stock.InvestmentRepository;
import eye.on.the.money.repository.stock.RSUTaxDetailsRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class RSUTaxServiceTest {

    private static final String USER_EMAIL = "test@test.test";
    private static final LocalDate VEST_DATE = LocalDate.of(2025, 3, 14);

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private RSUTaxDetailsRepository rsuTaxDetailsRepository;

    @Mock
    private TaxService taxService;

    private RSUTaxService service() {
        return new RSUTaxService(this.investmentRepository, this.rsuTaxDetailsRepository, this.taxService);
    }

    private Investment investment(long id, String buySell, LocalDate transactionDate, int quantity) {
        return Investment.builder()
                .id(id)
                .buySell(buySell)
                .transactionDate(transactionDate)
                .quantity(quantity)
                .stock(Stock.builder().id("aapl.us").shortName("AAPL").exchange("US").name("Apple").build())
                .user(User.builder().id(1L).email(USER_EMAIL).build())
                .build();
    }

    private RSUTaxDetails flagged(long id, boolean paid) {
        Investment investment = this.investment(id, "B", VEST_DATE, 50);
        investment.setRsu(true);
        return RSUTaxDetails.builder()
                .id(id)
                .investment(investment)
                .price(new BigDecimal("214.50"))
                .priceDate(VEST_DATE)
                .currency("USD")
                .taxDetails(TaxDetails.builder()
                        .rate(new BigDecimal("368.42"))
                        .rateDate(VEST_DATE)
                        .amountInHuf(new BigDecimal("3951304.50"))
                        .taxBase(new BigDecimal("3516661.01"))
                        .szocho(new BigDecimal("457166"))
                        .szja(new BigDecimal("527499"))
                        .total(new BigDecimal("984665"))
                        .calculatedOn(LocalDate.of(2026, 1, 1))
                        .paid(paid)
                        .build())
                .build();
    }

    private RSUTaxDTO valued() {
        return RSUTaxDTO.builder()
                .shortName("AAPL").exchange("US").date(VEST_DATE).quantity(50).currency("USD")
                .price(new BigDecimal("214.50")).priceDate(VEST_DATE)
                .amount(new BigDecimal("10725.00"))
                .rate(new BigDecimal("368.42")).rateDate(VEST_DATE)
                .amountInHuf(new BigDecimal("3951304.50"))
                .tax(TaxBreakdownDTO.builder()
                        .amount(new BigDecimal("3951304.50"))
                        .taxBase(new BigDecimal("3516661.01"))
                        .szocho(new BigDecimal("457166"))
                        .szja(new BigDecimal("527499"))
                        .total(new BigDecimal("984665"))
                        .build())
                .build();
    }

    private void stubValuation(RSUTaxDTO... items) {
        when(this.taxService.calculateTaxForRSUs(anyList()))
                .thenReturn(TaxReportDTO.builder().items(List.of(items)).build());
    }

    private List<RSUTaxDetails> savedDetails() {
        ArgumentCaptor<List<RSUTaxDetails>> captor = ArgumentCaptor.captor();
        verify(this.rsuTaxDetailsRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void storesTheValuationAndTheTaxWhenABuyIsFlagged() {
        Investment investment = this.investment(1L, "B", VEST_DATE, 50);
        when(this.investmentRepository.findByUserEmailAndIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of(investment));
        when(this.rsuTaxDetailsRepository.findByUserEmailAndInvestmentIdIn(anyString(), anyList()))
                .thenReturn(List.of());
        this.stubValuation(this.valued());

        this.service().setRSU(USER_EMAIL, List.of(1L), true);

        assertTrue(investment.isRsu());
        RSUTaxDetails saved = this.savedDetails().getFirst();
        assertEquals(investment, saved.getInvestment());
        assertEquals(0, new BigDecimal("214.50").compareTo(saved.getPrice()));
        assertEquals(VEST_DATE, saved.getPriceDate());
        assertEquals("USD", saved.getCurrency());

        TaxDetails details = saved.getTaxDetails();
        assertEquals(0, new BigDecimal("368.42").compareTo(details.getRate()));
        assertEquals(0, new BigDecimal("3951304.50").compareTo(details.getAmountInHuf()));
        assertEquals(0, new BigDecimal("984665").compareTo(details.getTotal()));
        assertEquals(LocalDate.now(), details.getCalculatedOn());
    }

    @Test
    void valuesTheGrantFromTheStockTheDateAndTheQuantity() {
        when(this.investmentRepository.findByUserEmailAndIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of(this.investment(1L, "B", VEST_DATE, 50)));
        when(this.rsuTaxDetailsRepository.findByUserEmailAndInvestmentIdIn(anyString(), anyList()))
                .thenReturn(List.of());
        this.stubValuation(this.valued());

        this.service().setRSU(USER_EMAIL, List.of(1L), true);

        ArgumentCaptor<List<RSUDTO>> captor = ArgumentCaptor.captor();
        verify(this.taxService).calculateTaxForRSUs(captor.capture());
        RSUDTO requested = captor.getValue().getFirst();
        assertEquals("AAPL", requested.getShortName());
        assertEquals("US", requested.getExchange());
        assertEquals(VEST_DATE, requested.getDate());
        assertEquals(50, requested.getQuantity());
    }

    @Test
    void skipsSellTransactions() {
        when(this.investmentRepository.findByUserEmailAndIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of(this.investment(1L, "S", VEST_DATE, 50)));

        this.service().setRSU(USER_EMAIL, List.of(1L), true);

        verifyNoInteractions(this.taxService);
        verifyNoInteractions(this.rsuTaxDetailsRepository);
        verify(this.investmentRepository, never()).saveAll(any());
    }

    @Test
    void flagsOnlyTheBuysInAMixedSelection() {
        Investment buy = this.investment(1L, "B", VEST_DATE, 50);
        Investment sell = this.investment(2L, "S", VEST_DATE, 10);
        when(this.investmentRepository.findByUserEmailAndIdIn(USER_EMAIL, List.of(1L, 2L)))
                .thenReturn(List.of(buy, sell));
        when(this.rsuTaxDetailsRepository.findByUserEmailAndInvestmentIdIn(anyString(), anyList()))
                .thenReturn(List.of());
        this.stubValuation(this.valued());

        this.service().setRSU(USER_EMAIL, List.of(1L, 2L), true);

        assertTrue(buy.isRsu());
        assertFalse(sell.isRsu());
        verify(this.investmentRepository).saveAll(List.of(buy));
        assertEquals(1, this.savedDetails().size());
    }

    @Test
    void dropsTheStoredTaxWhenTheFlagIsRemoved() {
        Investment investment = this.investment(1L, "B", VEST_DATE, 50);
        investment.setRsu(true);
        when(this.investmentRepository.findByUserEmailAndIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of(investment));

        this.service().setRSU(USER_EMAIL, List.of(1L), false);

        assertFalse(investment.isRsu());
        verify(this.rsuTaxDetailsRepository).deleteByInvestmentIdIn(List.of(1L));
        verify(this.rsuTaxDetailsRepository, never()).saveAll(any());
        verifyNoInteractions(this.taxService);
    }

    @Test
    void keepsThePaidFlagWhenTheTaxIsRecalculated() {
        RSUTaxDetails existing = this.flagged(1L, true);
        when(this.investmentRepository.findByUserEmailAndIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of(existing.getInvestment()));
        when(this.rsuTaxDetailsRepository.findByUserEmailAndInvestmentIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of(existing));
        this.stubValuation(this.valued());

        this.service().setRSU(USER_EMAIL, List.of(1L), true);

        RSUTaxDetails saved = this.savedDetails().getFirst();
        assertEquals(existing, saved);
        assertTrue(saved.getTaxDetails().isPaid());
        assertEquals(LocalDate.now(), saved.getTaxDetails().getCalculatedOn());
    }

    @Test
    void ignoresIdsThatBelongToAnotherUser() {
        when(this.investmentRepository.findByUserEmailAndIdIn(USER_EMAIL, List.of(1L))).thenReturn(List.of());

        this.service().setRSU(USER_EMAIL, List.of(1L), true);

        verifyNoInteractions(this.taxService);
        verifyNoInteractions(this.rsuTaxDetailsRepository);
    }

    @Test
    void returnsAnEmptyReportWhenNothingIsFlagged() {
        when(this.rsuTaxDetailsRepository.findFlaggedByUserEmail(USER_EMAIL)).thenReturn(List.of());

        RSUTaxEventReportDTO report = this.service().getRSUTaxEvents(USER_EMAIL);

        assertTrue(report.getItems().isEmpty());
        assertEquals(BigDecimal.ZERO, report.getTotalAmountInHuf());
        assertEquals(BigDecimal.ZERO, report.getTotalTax().getTotal());
    }

    @Test
    void reportsTheStoredTaxWithoutRevaluing() {
        when(this.rsuTaxDetailsRepository.findFlaggedByUserEmail(USER_EMAIL))
                .thenReturn(List.of(this.flagged(1L, false)));

        RSUTaxEventDTO item = this.service().getRSUTaxEvents(USER_EMAIL).getItems().getFirst();

        assertEquals(1L, item.getId());
        assertEquals("AAPL", item.getShortName());
        assertEquals("US", item.getExchange());
        assertEquals(50, item.getQuantity());
        assertEquals("USD", item.getCurrency());
        assertEquals(0, new BigDecimal("214.50").compareTo(item.getPrice()));
        assertEquals(0, new BigDecimal("10725.00").compareTo(item.getAmount()));
        assertEquals(0, new BigDecimal("3951304.50").compareTo(item.getAmountInHuf()));
        assertEquals(0, new BigDecimal("984665").compareTo(item.getTax().getTotal()));
        assertEquals(LocalDate.of(2026, 1, 1), item.getCalculatedOn());
        verifyNoInteractions(this.taxService);
    }

    @Test
    void addsUpTheTaxOfEveryFlaggedTransaction() {
        when(this.rsuTaxDetailsRepository.findFlaggedByUserEmail(USER_EMAIL))
                .thenReturn(List.of(this.flagged(1L, false), this.flagged(2L, false)));

        RSUTaxEventReportDTO report = this.service().getRSUTaxEvents(USER_EMAIL);

        assertEquals(2, report.getItems().size());
        assertEquals(0, new BigDecimal("7902609.00").compareTo(report.getTotalAmountInHuf()));
        assertEquals(0, new BigDecimal("1969330").compareTo(report.getTotalTax().getTotal()));
    }

    @Test
    void marksTheTaxAsPaid() {
        RSUTaxDetails details = this.flagged(1L, false);
        when(this.rsuTaxDetailsRepository.findByUserEmailAndInvestmentIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of(details));

        this.service().setTaxPaid(USER_EMAIL, List.of(1L), true);

        assertTrue(details.getTaxDetails().isPaid());
        verify(this.rsuTaxDetailsRepository).saveAll(List.of(details));
    }

    @Test
    void clearsThePaidFlagAgain() {
        RSUTaxDetails details = this.flagged(1L, true);
        when(this.rsuTaxDetailsRepository.findByUserEmailAndInvestmentIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of(details));

        this.service().setTaxPaid(USER_EMAIL, List.of(1L), false);

        assertFalse(details.getTaxDetails().isPaid());
    }

    @Test
    void ignoresTransactionsThatAreNotRSUsWhenMarkingAsPaid() {
        when(this.rsuTaxDetailsRepository.findByUserEmailAndInvestmentIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of());

        this.service().setTaxPaid(USER_EMAIL, List.of(1L), true);

        verify(this.rsuTaxDetailsRepository, never()).saveAll(any());
    }

    @Test
    void reportsWhetherTheTaxWasPaid() {
        when(this.rsuTaxDetailsRepository.findFlaggedByUserEmail(USER_EMAIL))
                .thenReturn(List.of(this.flagged(1L, true)));

        assertTrue(this.service().getRSUTaxEvents(USER_EMAIL).getItems().getFirst().isPaid());
    }

    @Test
    void writesTheReportAsCsv() {
        when(this.rsuTaxDetailsRepository.findFlaggedByUserEmail(USER_EMAIL))
                .thenReturn(List.of(this.flagged(1L, false)));
        StringWriter writer = new StringWriter();

        this.service().getCSV(USER_EMAIL, writer);

        String csv = writer.toString();
        assertTrue(csv.contains("Value (HUF)"));
        assertTrue(csv.contains("2025-03-14"));
        assertTrue(csv.contains("984665"));
    }
}
