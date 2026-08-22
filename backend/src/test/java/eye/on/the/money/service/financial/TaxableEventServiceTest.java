package eye.on.the.money.service.financial;

import eye.on.the.money.dto.out.TaxableEventDTO;
import eye.on.the.money.dto.out.TaxableEventReportDTO;
import eye.on.the.money.exception.TaxException;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.financial.BankTransaction;
import eye.on.the.money.model.financial.TaxDetails;
import eye.on.the.money.repository.financial.BankTransactionRepository;
import eye.on.the.money.service.api.MNBAPIService;
import eye.on.the.money.service.shared.TaxCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class TaxableEventServiceTest {

    private static final String USER_EMAIL = "test@test.test";

    @Mock
    private BankTransactionRepository bankTransactionRepository;

    @Mock
    private MNBAPIService mnbAPIService;

    private final TaxCalculator taxCalculator = new TaxCalculator();

    private TaxableEventService service() {
        return new TaxableEventService(this.bankTransactionRepository, this.mnbAPIService, this.taxCalculator);
    }

    private BankTransaction transaction(long id, LocalDate bookingDate, String currencyId, double amount) {
        return BankTransaction.builder()
                .id(id)
                .bankTransactionId("TX" + id)
                .bookingDate(bookingDate)
                .type("Utalas")
                .partnerName("PARTNER KFT")
                .memo("memo")
                .amount(amount)
                .currency(new Currency(currencyId, currencyId))
                .user(User.builder().id(1L).email(USER_EMAIL).build())
                .build();
    }

    private BankTransaction taxed(long id, LocalDate bookingDate, String currencyId, double amount,
                                  BigDecimal rate, String amountInHuf, String taxBase, String szocho,
                                  String szja, String total) {
        BankTransaction transaction = this.transaction(id, bookingDate, currencyId, amount);
        transaction.setTaxable(true);
        transaction.setTaxDetails(TaxDetails.builder()
                .rate(rate)
                .rateDate(bookingDate)
                .amountInHuf(new BigDecimal(amountInHuf))
                .taxBase(new BigDecimal(taxBase))
                .szocho(new BigDecimal(szocho))
                .szja(new BigDecimal(szja))
                .total(new BigDecimal(total))
                .calculatedOn(LocalDate.of(2026, 1, 1))
                .build());
        return transaction;
    }

    @Test
    void storesTheTaxWhenAForintTransactionIsFlagged() {
        BankTransaction transaction = this.transaction(1L, LocalDate.of(2025, 12, 1), "HUF", 1000000.0);
        when(this.bankTransactionRepository.findByUserEmailAndIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of(transaction));

        this.service().setTaxable(USER_EMAIL, List.of(1L), true);

        assertTrue(transaction.isTaxable());
        TaxDetails details = transaction.getTaxDetails();
        assertEquals(BigDecimal.ONE, details.getRate());
        assertEquals(0, new BigDecimal("1000000.00").compareTo(details.getAmountInHuf()));
        assertEquals(0, new BigDecimal("890000.00").compareTo(details.getTaxBase()));
        assertEquals(0, new BigDecimal("115700").compareTo(details.getSzocho()));
        assertEquals(0, new BigDecimal("133500").compareTo(details.getSzja()));
        assertEquals(0, new BigDecimal("249200").compareTo(details.getTotal()));
        assertEquals(LocalDate.now(), details.getCalculatedOn());
        verify(this.bankTransactionRepository).saveAll(List.of(transaction));
        verify(this.mnbAPIService, never()).getExchangeRates(anyCollection(), any(), any());
    }

    @Test
    void convertsOtherCurrenciesAtTheRateOfTheBookingDateWhenFlagged() {
        BankTransaction transaction = this.transaction(1L, LocalDate.of(2025, 12, 1), "EUR", 1000.0);
        when(this.bankTransactionRepository.findByUserEmailAndIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of(transaction));
        NavigableMap<LocalDate, BigDecimal> eur = new TreeMap<>();
        eur.put(LocalDate.of(2025, 12, 1), new BigDecimal("400"));
        when(this.mnbAPIService.getExchangeRates(anyCollection(), any(), any())).thenReturn(Map.of("EUR", eur));

        this.service().setTaxable(USER_EMAIL, List.of(1L), true);

        TaxDetails details = transaction.getTaxDetails();
        assertEquals(0, new BigDecimal("400000.00").compareTo(details.getAmountInHuf()));
        assertEquals(LocalDate.of(2025, 12, 1), details.getRateDate());
        assertEquals(0, new BigDecimal("99680").compareTo(details.getTotal()));
    }

    @Test
    void fallsBackToTheLastRatePublishedBeforeTheBookingDate() {
        BankTransaction transaction = this.transaction(1L, LocalDate.of(2025, 12, 7), "EUR", 1000.0);
        when(this.bankTransactionRepository.findByUserEmailAndIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of(transaction));
        NavigableMap<LocalDate, BigDecimal> eur = new TreeMap<>();
        eur.put(LocalDate.of(2025, 12, 5), new BigDecimal("400"));
        when(this.mnbAPIService.getExchangeRates(anyCollection(), any(), any())).thenReturn(Map.of("EUR", eur));

        this.service().setTaxable(USER_EMAIL, List.of(1L), true);

        assertEquals(LocalDate.of(2025, 12, 5), transaction.getTaxDetails().getRateDate());
        assertEquals(0, new BigDecimal("400000.00").compareTo(transaction.getTaxDetails().getAmountInHuf()));
    }

    @Test
    void complainsWhenNoRateIsAvailable() {
        when(this.bankTransactionRepository.findByUserEmailAndIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of(this.transaction(1L, LocalDate.of(2025, 12, 1), "EUR", 1000.0)));
        when(this.mnbAPIService.getExchangeRates(anyCollection(), any(), any())).thenReturn(Map.of());

        TaxableEventService service = this.service();
        List<Long> ids = List.of(1L);

        assertThrows(TaxException.class, () -> service.setTaxable(USER_EMAIL, ids, true));
        verify(this.bankTransactionRepository, never()).saveAll(any());
    }

    @Test
    void leavesOutgoingTransactionsUntaxed() {
        BankTransaction transaction = this.transaction(1L, LocalDate.of(2025, 12, 1), "HUF", -1000.0);
        when(this.bankTransactionRepository.findByUserEmailAndIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of(transaction));

        this.service().setTaxable(USER_EMAIL, List.of(1L), true);

        assertEquals(0, BigDecimal.ZERO.compareTo(transaction.getTaxDetails().getTotal()));
    }

    @Test
    void recalculatesWhenAnAlreadyFlaggedTransactionIsFlaggedAgain() {
        BankTransaction transaction = this.taxed(1L, LocalDate.of(2025, 12, 1), "HUF", 1000000.0,
                BigDecimal.ONE, "1.00", "1.00", "1", "1", "2");
        when(this.bankTransactionRepository.findByUserEmailAndIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of(transaction));

        this.service().setTaxable(USER_EMAIL, List.of(1L), true);

        assertEquals(0, new BigDecimal("249200").compareTo(transaction.getTaxDetails().getTotal()));
    }

    @Test
    void dropsTheStoredTaxWhenTheFlagIsRemoved() {
        BankTransaction transaction = this.taxed(1L, LocalDate.of(2025, 12, 1), "HUF", 1000000.0,
                BigDecimal.ONE, "1000000.00", "890000.00", "115700", "133500", "249200");
        when(this.bankTransactionRepository.findByUserEmailAndIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of(transaction));

        this.service().setTaxable(USER_EMAIL, List.of(1L), false);

        assertFalse(transaction.isTaxable());
        assertNull(transaction.getTaxDetails());
        verify(this.mnbAPIService, never()).getExchangeRates(anyCollection(), any(), any());
    }

    @Test
    void ignoresIdsThatBelongToAnotherUser() {
        when(this.bankTransactionRepository.findByUserEmailAndIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of());

        this.service().setTaxable(USER_EMAIL, List.of(1L), true);

        verify(this.bankTransactionRepository, never()).saveAll(any());
    }

    @Test
    void returnsAnEmptyReportWhenNothingIsFlagged() {
        when(this.bankTransactionRepository.findByUserEmailAndTaxableTrueOrderByBookingDateDesc(USER_EMAIL))
                .thenReturn(List.of());

        TaxableEventReportDTO report = this.service().getTaxableEvents(USER_EMAIL);

        assertTrue(report.getItems().isEmpty());
        assertEquals(BigDecimal.ZERO, report.getTotalAmountInHuf());
        assertEquals(BigDecimal.ZERO, report.getTotalTax().getTotal());
    }

    @Test
    void reportsTheStoredTaxWithoutRecalculating() {
        when(this.bankTransactionRepository.findByUserEmailAndTaxableTrueOrderByBookingDateDesc(USER_EMAIL))
                .thenReturn(List.of(this.taxed(1L, LocalDate.of(2025, 12, 1), "EUR", 1000.0,
                        new BigDecimal("400"), "400000.00", "356000.00", "46280", "53400", "99680")));

        TaxableEventDTO item = this.service().getTaxableEvents(USER_EMAIL).getItems().getFirst();

        assertEquals(0, new BigDecimal("400").compareTo(item.getRate()));
        assertEquals(0, new BigDecimal("400000.00").compareTo(item.getAmountInHuf()));
        assertEquals(0, new BigDecimal("99680").compareTo(item.getTax().getTotal()));
        assertEquals(LocalDate.of(2026, 1, 1), item.getCalculatedOn());
        verify(this.mnbAPIService, never()).getExchangeRates(anyCollection(), any(), any());
    }

    @Test
    void addsUpTheTaxOfEveryFlaggedTransaction() {
        when(this.bankTransactionRepository.findByUserEmailAndTaxableTrueOrderByBookingDateDesc(USER_EMAIL))
                .thenReturn(List.of(
                        this.taxed(1L, LocalDate.of(2025, 12, 1), "HUF", 1000000.0, BigDecimal.ONE,
                                "1000000.00", "890000.00", "115700", "133500", "249200"),
                        this.taxed(2L, LocalDate.of(2025, 11, 1), "HUF", 500000.0, BigDecimal.ONE,
                                "500000.00", "445000.00", "57850", "66750", "124600")));

        TaxableEventReportDTO report = this.service().getTaxableEvents(USER_EMAIL);

        assertEquals(2, report.getItems().size());
        assertEquals(0, new BigDecimal("1500000.00").compareTo(report.getTotalAmountInHuf()));
        assertEquals(0, new BigDecimal("373800").compareTo(report.getTotalTax().getTotal()));
    }

    @Test
    void marksTheTaxAsPaid() {
        BankTransaction transaction = this.taxed(1L, LocalDate.of(2025, 12, 1), "HUF", 1000000.0,
                BigDecimal.ONE, "1000000.00", "890000.00", "115700", "133500", "249200");
        when(this.bankTransactionRepository.findByUserEmailAndIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of(transaction));

        this.service().setTaxPaid(USER_EMAIL, List.of(1L), true);

        assertTrue(transaction.getTaxDetails().isPaid());
        verify(this.bankTransactionRepository).saveAll(List.of(transaction));
    }

    @Test
    void clearsThePaidFlagAgain() {
        BankTransaction transaction = this.taxed(1L, LocalDate.of(2025, 12, 1), "HUF", 1000000.0,
                BigDecimal.ONE, "1000000.00", "890000.00", "115700", "133500", "249200");
        transaction.getTaxDetails().setPaid(true);
        when(this.bankTransactionRepository.findByUserEmailAndIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of(transaction));

        this.service().setTaxPaid(USER_EMAIL, List.of(1L), false);

        assertFalse(transaction.getTaxDetails().isPaid());
    }

    @Test
    void ignoresTransactionsThatAreNotTaxableEventsWhenMarkingAsPaid() {
        BankTransaction transaction = this.transaction(1L, LocalDate.of(2025, 12, 1), "HUF", 1000000.0);
        when(this.bankTransactionRepository.findByUserEmailAndIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of(transaction));

        this.service().setTaxPaid(USER_EMAIL, List.of(1L), true);

        assertNull(transaction.getTaxDetails());
        verify(this.bankTransactionRepository, never()).saveAll(any());
    }

    @Test
    void keepsThePaidFlagWhenTheTaxIsRecalculated() {
        BankTransaction transaction = this.taxed(1L, LocalDate.of(2025, 12, 1), "HUF", 1000000.0,
                BigDecimal.ONE, "1.00", "1.00", "1", "1", "2");
        transaction.getTaxDetails().setPaid(true);
        when(this.bankTransactionRepository.findByUserEmailAndIdIn(USER_EMAIL, List.of(1L)))
                .thenReturn(List.of(transaction));

        this.service().setTaxable(USER_EMAIL, List.of(1L), true);

        assertEquals(0, new BigDecimal("249200").compareTo(transaction.getTaxDetails().getTotal()));
        assertTrue(transaction.getTaxDetails().isPaid());
    }

    @Test
    void reportsWhetherTheTaxWasPaid() {
        BankTransaction transaction = this.taxed(1L, LocalDate.of(2025, 12, 1), "HUF", 1000000.0,
                BigDecimal.ONE, "1000000.00", "890000.00", "115700", "133500", "249200");
        transaction.getTaxDetails().setPaid(true);
        when(this.bankTransactionRepository.findByUserEmailAndTaxableTrueOrderByBookingDateDesc(USER_EMAIL))
                .thenReturn(List.of(transaction));

        assertTrue(this.service().getTaxableEvents(USER_EMAIL).getItems().getFirst().isPaid());
    }

    @Test
    void writesTheReportAsCsv() {
        when(this.bankTransactionRepository.findByUserEmailAndTaxableTrueOrderByBookingDateDesc(USER_EMAIL))
                .thenReturn(List.of(this.taxed(1L, LocalDate.of(2025, 12, 1), "HUF", 1000000.0, BigDecimal.ONE,
                        "1000000.00", "890000.00", "115700", "133500", "249200")));
        StringWriter writer = new StringWriter();

        this.service().getCSV(USER_EMAIL, writer);

        String csv = writer.toString();
        assertTrue(csv.contains("Amount (HUF)"));
        assertTrue(csv.contains("2025-12-01"));
        assertTrue(csv.contains("249200"));
    }
}
