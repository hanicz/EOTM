package eye.on.the.money.service.shared;

import eye.on.the.money.dto.out.AssetClassValueDTO;
import eye.on.the.money.dto.out.MonthlyPerformanceDTO;
import eye.on.the.money.dto.out.NetWorthDTO;
import eye.on.the.money.dto.out.NetWorthHistoryDTO;
import eye.on.the.money.dto.out.NetWorthPointDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.User;
import eye.on.the.money.model.networth.NetWorthSnapshot;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.repository.networth.NetWorthSnapshotRepository;
import eye.on.the.money.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NetWorthSnapshotServiceTest {

    private static final Long USER = 7L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 31);

    @Mock
    private NetWorthSnapshotRepository netWorthSnapshotRepository;
    @Mock
    private NetWorthService netWorthService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;

    private NetWorthSnapshotService netWorthSnapshotService;

    @BeforeEach
    void setUp() {
        this.netWorthSnapshotService = new NetWorthSnapshotService(this.netWorthSnapshotRepository,
                this.netWorthService, this.userRepository, this.userService);

        when(this.userService.getReference(anyLong()))
                .thenAnswer(invocation -> User.builder().id(invocation.getArgument(0)).build());
        when(this.netWorthSnapshotRepository.findByUserIdAndSnapshotDate(anyLong(), any()))
                .thenReturn(List.of());
        when(this.netWorthService.getNetWorth(anyLong(), anyString(), anyBoolean()))
                .thenReturn(this.netWorth(Map.of("Stock", new double[]{1000, 1250}, "Cash", new double[]{300, 300})));
    }

    @Test
    void capture_storesOneHufRowPerAssetClass() {
        this.netWorthSnapshotService.capture(USER, TODAY, true);

        verify(this.netWorthService).getNetWorth(USER, "HUF", true);
        List<NetWorthSnapshot> saved = this.savedRows();
        assertEquals(2, saved.size());

        NetWorthSnapshot stock = this.rowFor(saved, "Stock");
        assertEquals(TODAY, stock.getSnapshotDate());
        assertEquals(1000.0, stock.getSpent());
        assertEquals(1250.0, stock.getWorth());
        assertEquals(USER, stock.getUser().getId());

        NetWorthSnapshot cash = this.rowFor(saved, "Cash");
        assertEquals(300.0, cash.getSpent());
        assertEquals(300.0, cash.getWorth());
    }

    @Test
    void capture_overwritesTheRowsAlreadyStoredForThatDay() {
        NetWorthSnapshot existingStock = this.row(TODAY, "Stock", 900, 950);
        existingStock.setId(55L);
        when(this.netWorthSnapshotRepository.findByUserIdAndSnapshotDate(USER, TODAY))
                .thenReturn(List.of(existingStock));

        this.netWorthSnapshotService.capture(USER, TODAY, false);

        List<NetWorthSnapshot> saved = this.savedRows();
        assertEquals(2, saved.size());
        NetWorthSnapshot stock = this.rowFor(saved, "Stock");
        assertSame(existingStock, stock);
        assertEquals(1000.0, stock.getSpent());
        assertEquals(1250.0, stock.getWorth());
    }

    @Test
    void captureAll_keepsGoingWhenOneUserFails() {
        User failing = User.builder().id(1L).email("first@example.com").build();
        User working = User.builder().id(2L).email("second@example.com").build();
        when(this.userRepository.findAll()).thenReturn(List.of(failing, working));
        when(this.netWorthService.getNetWorth(1L, "HUF", true)).thenThrow(new APIException("Quote service down"));

        this.netWorthSnapshotService.captureAll(TODAY);

        verify(this.netWorthService).getNetWorth(2L, "HUF", true);
        verify(this.netWorthSnapshotRepository, times(1)).saveAll(anyList());
    }

    @Test
    void getHistory_capturesTodayWhenItIsMissing() {
        when(this.netWorthSnapshotRepository.existsByUserIdAndSnapshotDate(USER, TODAY)).thenReturn(false);
        when(this.netWorthSnapshotRepository.findByUserIdOrderBySnapshotDate(USER)).thenReturn(List.of());

        this.netWorthSnapshotService.getHistory(USER, TODAY);

        verify(this.netWorthService).getNetWorth(USER, "HUF", false);
    }

    @Test
    void getHistory_skipsTheCaptureWhenTodayIsAlreadyStored() {
        when(this.netWorthSnapshotRepository.existsByUserIdAndSnapshotDate(USER, TODAY)).thenReturn(true);
        when(this.netWorthSnapshotRepository.findByUserIdOrderBySnapshotDate(USER)).thenReturn(List.of());

        this.netWorthSnapshotService.getHistory(USER, TODAY);

        verify(this.netWorthService, never()).getNetWorth(anyLong(), anyString(), anyBoolean());
    }

    @Test
    void getHistory_returnsTheStoredHistoryWhenTodaysCaptureFails() {
        when(this.netWorthSnapshotRepository.existsByUserIdAndSnapshotDate(USER, TODAY)).thenReturn(false);
        when(this.netWorthService.getNetWorth(USER, "HUF", false)).thenThrow(new APIException("Quote service down"));
        when(this.netWorthSnapshotRepository.findByUserIdOrderBySnapshotDate(USER))
                .thenReturn(List.of(this.row(LocalDate.of(2026, 8, 30), "Stock", 100, 110)));

        NetWorthHistoryDTO history = this.netWorthSnapshotService.getHistory(USER, TODAY);

        assertEquals(1, history.getPoints().size());
    }

    @Test
    void getHistory_sumsEveryAssetClassIntoOnePointPerDay() {
        this.storedHistory(List.of(
                this.row(LocalDate.of(2026, 8, 1), "Stock", 1000, 1100),
                this.row(LocalDate.of(2026, 8, 1), "Cash", 500, 500),
                this.row(LocalDate.of(2026, 8, 2), "Stock", 1000, 1150),
                this.row(LocalDate.of(2026, 8, 2), "Cash", 500, 500)));

        NetWorthHistoryDTO history = this.netWorthSnapshotService.getHistory(USER, TODAY);

        assertEquals("HUF", history.getCurrency());
        assertEquals(2, history.getPoints().size());
        NetWorthPointDTO first = history.getPoints().getFirst();
        assertEquals(LocalDate.of(2026, 8, 1), first.getDate());
        assertEquals(new BigDecimal("1500.00"), first.getTotalSpent());
        assertEquals(new BigDecimal("1600.00"), first.getTotalWorth());
        assertEquals(new BigDecimal("1100.00"), first.getAssetWorth().get("Stock"));
        assertEquals(new BigDecimal("500.00"), first.getAssetWorth().get("Cash"));
        assertEquals(new BigDecimal("1650.00"), history.getPoints().getLast().getTotalWorth());
    }

    @Test
    void getHistory_splitsEachMonthIntoContributionsAndMarketGain() {
        this.storedHistory(List.of(
                this.row(LocalDate.of(2026, 7, 1), "Stock", 1000, 1000),
                this.row(LocalDate.of(2026, 7, 1), "Cash", 500, 500),
                this.row(LocalDate.of(2026, 7, 31), "Stock", 1000, 1100),
                this.row(LocalDate.of(2026, 7, 31), "Cash", 500, 500),
                this.row(LocalDate.of(2026, 8, 31), "Stock", 1200, 1400),
                this.row(LocalDate.of(2026, 8, 31), "Cash", 700, 700)));

        List<MonthlyPerformanceDTO> months = this.netWorthSnapshotService.getHistory(USER, TODAY).getMonths();

        assertEquals(2, months.size());

        MonthlyPerformanceDTO august = months.getFirst();
        assertEquals("2026-08", august.getMonth());
        assertEquals(new BigDecimal("2100.00"), august.getEndWorth());
        assertEquals(new BigDecimal("1900.00"), august.getEndSpent());
        assertEquals(new BigDecimal("500.00"), august.getChange());
        assertEquals(new BigDecimal("31.25"), august.getChangePct());
        assertEquals(new BigDecimal("400.00"), august.getContributions());
        assertEquals(new BigDecimal("100.00"), august.getMarketGain());

        MonthlyPerformanceDTO july = months.getLast();
        assertEquals("2026-07", july.getMonth());
        assertEquals(new BigDecimal("100.00"), july.getChange());
        assertEquals(new BigDecimal("6.67"), july.getChangePct());
        assertEquals(new BigDecimal("0.00"), july.getContributions());
        assertEquals(new BigDecimal("100.00"), july.getMarketGain());
    }

    @Test
    void getHistory_leavesTheChangePercentEmptyWhenTheMonthStartedAtZero() {
        this.storedHistory(List.of(
                this.row(LocalDate.of(2026, 8, 1), "Stock", 0, 0),
                this.row(LocalDate.of(2026, 8, 20), "Stock", 400, 420)));

        MonthlyPerformanceDTO august = this.netWorthSnapshotService.getHistory(USER, TODAY).getMonths().getFirst();

        assertEquals(new BigDecimal("420.00"), august.getChange());
        assertNull(august.getChangePct());
        assertEquals(new BigDecimal("400.00"), august.getContributions());
        assertEquals(new BigDecimal("20.00"), august.getMarketGain());
    }

    private void storedHistory(List<NetWorthSnapshot> rows) {
        when(this.netWorthSnapshotRepository.existsByUserIdAndSnapshotDate(USER, TODAY)).thenReturn(true);
        when(this.netWorthSnapshotRepository.findByUserIdOrderBySnapshotDate(USER)).thenReturn(rows);
    }

    @SuppressWarnings("unchecked")
    private List<NetWorthSnapshot> savedRows() {
        ArgumentCaptor<List<NetWorthSnapshot>> captor = ArgumentCaptor.forClass(List.class);
        verify(this.netWorthSnapshotRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private NetWorthSnapshot rowFor(List<NetWorthSnapshot> rows, String assetClass) {
        return rows.stream().filter(row -> assetClass.equals(row.getAssetClass())).findFirst().orElseThrow();
    }

    private NetWorthSnapshot row(LocalDate date, String assetClass, double spent, double worth) {
        return NetWorthSnapshot.builder()
                .user(User.builder().id(USER).build())
                .snapshotDate(date)
                .assetClass(assetClass)
                .spent(spent)
                .worth(worth)
                .build();
    }

    private NetWorthDTO netWorth(Map<String, double[]> values) {
        List<AssetClassValueDTO> assets = new ArrayList<>();
        values.forEach((assetClass, amounts) -> assets.add(AssetClassValueDTO.builder()
                .assetClass(assetClass)
                .spent(BigDecimal.valueOf(amounts[0]))
                .worth(BigDecimal.valueOf(amounts[1]))
                .build()));
        return NetWorthDTO.builder().currency("HUF").assets(assets).build();
    }
}
