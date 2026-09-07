package eye.on.the.money.dto.out;

import eye.on.the.money.model.stock.VestingFrequency;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RSUGrantReportDTOSerializationTest {

    private RSUVestDTO vest(int sequence, LocalDate vestDate, boolean vested) {
        return RSUVestDTO.builder()
                .grantId(1L).shortName("ACME").exchange("US")
                .grantDate(LocalDate.of(2024, 3, 1))
                .sequence(sequence).vestDate(vestDate).quantity(100)
                .currency("USD").price(new BigDecimal("100.25")).priceDate(vestDate)
                .amount(new BigDecimal("10025.00")).rate(new BigDecimal("350.12")).rateDate(vestDate)
                .amountInHuf(new BigDecimal("3509953.00")).netInHuf(new BigDecimal("2562266.00"))
                .vested(vested).projected(!vested)
                .tax(TaxBreakdownDTO.builder().amount(new BigDecimal("3509953.00"))
                        .taxBase(new BigDecimal("3123858.17")).szocho(new BigDecimal("406102"))
                        .szja(new BigDecimal("468579")).total(new BigDecimal("874681")).build())
                .build();
    }

    private RSUGrantReportDTO report() {
        RSUGrantDTO grant = RSUGrantDTO.builder()
                .id(1L).shortName("ACME").exchange("US").currency(null)
                .grantDate(LocalDate.of(2024, 3, 1)).quantity(400)
                .vestingYears(4).vestingFrequency(VestingFrequency.ANNUAL).note("joining grant")
                .vests(List.of(this.vest(1, LocalDate.of(2025, 3, 1), true),
                        this.vest(2, LocalDate.of(2026, 3, 1), false)))
                .totalAmountInHuf(new BigDecimal("7019906.00"))
                .totalTax(TaxBreakdownDTO.builder().amount(new BigDecimal("7019906.00"))
                        .taxBase(new BigDecimal("6247716.34")).szocho(new BigDecimal("812204"))
                        .szja(new BigDecimal("937158")).total(new BigDecimal("1749362")).build())
                .totalNetInHuf(new BigDecimal("5124532.00"))
                .vestedNetInHuf(new BigDecimal("2562266.00"))
                .upcomingNetInHuf(new BigDecimal("2562266.00"))
                .error(null)
                .build();

        return RSUGrantReportDTO.builder().items(List.of(grant))
                .totalAmountInHuf(new BigDecimal("7019906.00"))
                .totalTax(grant.getTotalTax())
                .totalNetInHuf(new BigDecimal("5124532.00"))
                .build();
    }

    private RSUGrantReportDTO roundTrip(RSUGrantReportDTO source) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(source);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (RSUGrantReportDTO) in.readObject();
        }
    }

    @Test
    void survivesTheJdkSerializationRedisUsesForTheGrantsCache() throws Exception {
        RSUGrantReportDTO restored = this.roundTrip(this.report());

        assertEquals(1, restored.getItems().size());
        assertEquals(new BigDecimal("7019906.00"), restored.getTotalAmountInHuf());
        assertEquals(new BigDecimal("5124532.00"), restored.getTotalNetInHuf());
        assertEquals(new BigDecimal("1749362"), restored.getTotalTax().getTotal());
    }

    @Test
    void keepsEveryVestFieldTheWidgetAndSchedulePageRead() throws Exception {
        RSUGrantDTO restored = this.roundTrip(this.report()).getItems().getFirst();

        assertEquals("ACME", restored.getShortName());
        assertEquals(VestingFrequency.ANNUAL, restored.getVestingFrequency());
        assertEquals(LocalDate.of(2024, 3, 1), restored.getGrantDate());
        assertEquals(2, restored.getVests().size());

        RSUVestDTO upcoming = restored.getVests().get(1);
        assertEquals(LocalDate.of(2026, 3, 1), upcoming.getVestDate());
        assertEquals(100, upcoming.getQuantity());
        assertEquals(new BigDecimal("2562266.00"), upcoming.getNetInHuf());
        assertFalse(upcoming.isVested());
        assertTrue(upcoming.isProjected());
        assertEquals(new BigDecimal("874681"), upcoming.getTax().getTotal());
    }

    @Test
    void survivesAnEmptyReport() throws Exception {
        RSUGrantReportDTO restored = this.roundTrip(RSUGrantReportDTO.empty());

        assertEquals(List.of(), restored.getItems());
        assertEquals(BigDecimal.ZERO, restored.getTotalNetInHuf());
    }
}
