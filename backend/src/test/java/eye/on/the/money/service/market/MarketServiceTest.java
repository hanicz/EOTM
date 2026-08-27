package eye.on.the.money.service.market;

import eye.on.the.money.dto.out.MarketExchangeDTO;
import eye.on.the.money.dto.out.MarketHolidayDTO;
import eye.on.the.money.model.market.MarketExchange;
import eye.on.the.money.model.market.MarketHoliday;
import eye.on.the.money.repository.market.MarketExchangeRepository;
import eye.on.the.money.repository.market.MarketHolidayRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class MarketServiceTest {

    @Mock
    private MarketExchangeRepository marketExchangeRepository;
    @Mock
    private MarketHolidayRepository marketHolidayRepository;

    @InjectMocks
    private MarketService marketService;

    @Test
    void getExchanges_mapsHoursAndCode() {
        this.givenExchanges(this.us());
        this.givenHolidays();

        MarketExchangeDTO dto = this.marketService.getExchanges().getFirst();

        assertEquals("US", dto.getCode());
        assertEquals("NYSE / NASDAQ", dto.getName());
        assertEquals("America/New_York", dto.getTimeZone());
        assertEquals(LocalTime.of(9, 30), dto.getOpenTime());
        assertEquals(LocalTime.of(16, 0), dto.getCloseTime());
        assertEquals("USD", dto.getCurrency());
    }

    @Test
    void getExchanges_groupsHolidaysUnderTheirOwnExchange() {
        MarketExchange us = this.us();
        MarketExchange lse = this.lse();
        this.givenExchanges(us, lse);
        this.givenHolidays(
                this.holiday(us, "2026-12-25", "Christmas Day", null),
                this.holiday(lse, "2026-12-28", "Boxing Day (substitute)", null));

        List<MarketExchangeDTO> exchanges = this.marketService.getExchanges();

        assertEquals(1, exchanges.getFirst().getHolidays().size());
        assertEquals("Christmas Day", exchanges.getFirst().getHolidays().getFirst().getName());
        assertEquals(1, exchanges.get(1).getHolidays().size());
        assertEquals("Boxing Day (substitute)", exchanges.get(1).getHolidays().getFirst().getName());
    }

    @Test
    void getExchanges_keepsEarlyCloseAndFullClosureApart() {
        MarketExchange us = this.us();
        this.givenExchanges(us);
        this.givenHolidays(
                this.holiday(us, "2026-11-26", "Thanksgiving Day", null),
                this.holiday(us, "2026-11-27", "Day after Thanksgiving", LocalTime.of(13, 0)));

        List<MarketHolidayDTO> holidays = this.marketService.getExchanges().getFirst().getHolidays();

        assertNull(holidays.getFirst().getCloseTime());
        assertEquals(LocalTime.of(13, 0), holidays.get(1).getCloseTime());
    }

    @Test
    void getExchanges_returnsEmptyHolidayListWhenNoneUpcoming() {
        this.givenExchanges(this.us());
        this.givenHolidays();

        assertTrue(this.marketService.getExchanges().getFirst().getHolidays().isEmpty());
    }

    private void givenExchanges(MarketExchange... exchanges) {
        when(this.marketExchangeRepository.findAllByOrderByIdAsc()).thenReturn(List.of(exchanges));
    }

    private void givenHolidays(MarketHoliday... holidays) {
        when(this.marketHolidayRepository.findByHolidayDateGreaterThanEqualOrderByHolidayDateAsc(any()))
                .thenReturn(List.of(holidays));
    }

    private MarketExchange us() {
        return MarketExchange.builder()
                .id("US")
                .name("NYSE / NASDAQ")
                .timeZone("America/New_York")
                .openTime(LocalTime.of(9, 30))
                .closeTime(LocalTime.of(16, 0))
                .currency("USD")
                .countryISO2("US")
                .build();
    }

    private MarketExchange lse() {
        return MarketExchange.builder()
                .id("LSE")
                .name("London Stock Exchange")
                .timeZone("Europe/London")
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(16, 30))
                .currency("GBP")
                .countryISO2("GB")
                .build();
    }

    private MarketHoliday holiday(MarketExchange exchange, String date, String name, LocalTime closeTime) {
        return MarketHoliday.builder()
                .exchange(exchange)
                .holidayDate(LocalDate.parse(date))
                .name(name)
                .closeTime(closeTime)
                .build();
    }
}
