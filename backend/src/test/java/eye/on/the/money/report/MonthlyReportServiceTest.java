package eye.on.the.money.report;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.dto.out.MonthlyReportDTO;
import eye.on.the.money.dto.out.NetWorthDTO;
import eye.on.the.money.service.report.MonthlyReportService;
import eye.on.the.money.service.shared.NetWorthService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class MonthlyReportServiceTest {

    private static final String USER_EMAIL = "test@test.test";

    @Autowired
    private UserRepository userRepository;

    private Long user;

    @MockitoBean
    private NetWorthService netWorthService;

    @Autowired
    private MonthlyReportService monthlyReportService;

    @BeforeEach
    public void setUpEach() {
        this.user = this.userRepository.findByEmail(USER_EMAIL).getId();
        when(this.netWorthService.getNetWorth(anyLong(), anyString(), anyBoolean()))
                .thenReturn(NetWorthDTO.builder().currency("EUR").assets(List.of()).build());
    }

    @Test
    public void buildKeepsOnlyTheReportMonthsTrades() {
        MonthlyReportDTO report = this.monthlyReportService.build(this.user, YearMonth.of(2023, 9), "EUR");

        Assertions.assertAll("September 2023",
                () -> assertEquals(2023, report.getYear()),
                () -> assertEquals(9, report.getMonth()),
                () -> assertEquals(3, report.getActivity().stockTrades().size()),
                () -> assertTrue(report.getActivity().stockTrades().stream()
                        .allMatch(trade -> trade.getTransactionDate().getMonthValue() == 9)),
                () -> assertTrue(report.getActivity().cryptoTrades().isEmpty()),
                () -> assertTrue(report.getActivity().forexTrades().isEmpty()),
                () -> assertTrue(report.getActivity().dividends().isEmpty()));
    }

    @Test
    public void buildKeepsOnlyTheReportMonthsDividends() {
        MonthlyReportDTO report = this.monthlyReportService.build(this.user, YearMonth.of(2021, 6), "EUR");

        Assertions.assertAll("June 2021",
                () -> assertEquals(1, report.getActivity().dividends().size()),
                () -> assertEquals(225.0, report.getActivity().dividends().getFirst().getAmount()),
                () -> assertEquals(1, report.getActivity().dividendTotals().size()),
                () -> assertEquals("HUF", report.getActivity().dividendTotals().getFirst().currencyId()),
                () -> assertEquals(225.0, report.getActivity().dividendTotals().getFirst().amount()),
                () -> assertTrue(report.getActivity().stockTrades().isEmpty()));
    }

    @Test
    public void buildFindsCryptoTradesInTheirOwnMonth() {
        MonthlyReportDTO report = this.monthlyReportService.build(this.user, YearMonth.of(2021, 5), "EUR");

        Assertions.assertAll("May 2021",
                () -> assertEquals(6, report.getActivity().cryptoTrades().size()),
                () -> assertEquals(6, report.getActivity().tradeCount()),
                () -> assertTrue(report.getActivity().stockTrades().isEmpty()));
    }

    @Test
    public void buildReportsAnEmptyMonthAsEmpty() {
        MonthlyReportDTO report = this.monthlyReportService.build(this.user, YearMonth.of(2020, 1), "EUR");

        Assertions.assertAll("January 2020",
                () -> assertTrue(report.getActivity().isEmpty()),
                () -> assertEquals(0, report.getActivity().tradeCount()),
                () -> assertTrue(report.getCashFlow().isEmpty()));
    }

    @Test
    public void buildAsksForFreshPrices() {
        this.monthlyReportService.build(this.user, YearMonth.of(2023, 9), "HUF");

        verify(this.netWorthService).getNetWorth(this.user, "HUF", true);
    }
}
