package eye.on.the.money.service.security;

import eye.on.the.money.dto.out.ImportResultDTO;
import eye.on.the.money.exception.CSVException;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.security.Interest;
import eye.on.the.money.model.security.Security;
import eye.on.the.money.model.security.SecurityTransaction;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.security.InterestRepository;
import eye.on.the.money.repository.security.SecurityTransactionRepository;
import eye.on.the.money.service.user.UserService;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebkincstarImportServiceTest {

    private static final Long USER_ID = 1L;
    private static final String ACCOUNT = "00000000";
    private static final String BOND_NAME = "Prémium Magyar Állampapír 2099/Z";
    private static final String BOND_ID = "PMÁP 2099/Z";

    private static final String[] HEADERS = {
            "Számlaszám", "Számla típusa", "Tranzakció típusa", "Tranzakció azonosítója", "Instrumentum",
            "Tranzakció státusza", "Névérték", "Összeg", "Devizanem", "Értéknap"};

    @Mock
    private SecurityTransactionRepository securityTransactionRepository;
    @Mock
    private InterestRepository interestRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private SecurityService securityService;
    @Mock
    private UserService userService;

    @InjectMocks
    private WebkincstarImportService webkincstarImportService;

    private final User user = User.builder().id(USER_ID).email("test@email.com").build();
    private final Currency huf = new Currency("HUF", "forint");
    private final Security bond = Security.builder().id(BOND_ID).name(BOND_NAME).build();

    @BeforeEach
    void setUp() {
        when(this.userService.getReference(USER_ID)).thenReturn(this.user);
        when(this.currencyRepository.findById("HUF")).thenReturn(Optional.of(this.huf));
        when(this.securityService.getOrCreateByName(BOND_NAME)).thenReturn(this.bond);
        when(this.securityTransactionRepository
                .findByUserIdAndSecurity_IdAndTransactionDateAndBuySellAndQuantityAndAmountOrderById(
                        anyLong(), anyString(), any(), anyString(), anyInt(), any()))
                .thenReturn(List.of());
        when(this.interestRepository
                .findByUserIdAndSecurity_IdAndInterestDateAndAmountAndCurrency_IdOrderById(
                        anyLong(), anyString(), any(), any(), anyString()))
                .thenReturn(List.of());
    }

    private Object[] row(String type, Object nominal, Object amount, String valueDate) {
        return new Object[]{ACCOUNT, "Értékpapír nyilvántartási-számla (" + ACCOUNT + ")", type,
                "TST0000000000001", BOND_NAME, "Lezárt", nominal, amount, "HUF", valueDate};
    }

    private MockMultipartFile file(Object[]... rows) {
        return this.file(HEADERS, rows);
    }

    private MockMultipartFile file(String[] headers, Object[]... rows) {
        try (HSSFWorkbook workbook = new HSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("new sheet");
            Row headerRow = sheet.createRow(0);
            for (int column = 0; column < headers.length; column++) {
                headerRow.createCell(column).setCellValue(headers[column]);
            }
            for (int index = 0; index < rows.length; index++) {
                Row row = sheet.createRow(index + 1);
                Object[] values = rows[index];
                for (int column = 0; column < values.length; column++) {
                    if (values[column] instanceof Double number) {
                        row.createCell(column).setCellValue(number);
                    } else if (values[column] != null) {
                        row.createCell(column).setCellValue((String) values[column]);
                    }
                }
            }
            workbook.write(output);
            return new MockMultipartFile("file", "transaction.xls",
                    "application/vnd.ms-excel", output.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void processXls_importsTargetTypesAndSkipsTheRest() {
        MockMultipartFile file = this.file(
                this.row("Vétel", "1 000 000", "1 012 842", "2026.01.20."),
                this.row("Eladás", "1 000 000", "1 020 000", "2026.06.30."),
                this.row("Esedékesség fizetés", "1 000 000", "25 000", "2026.03.20."),
                this.row("Pénzszámla befizetés", "", "25 000", "2026.03.20."),
                this.row("Pénzszámla kifizetés", "", "1 012 842", "2026.01.20."),
                this.row("Utalás érkeztetés", "", "2 000 000", "2026.01.19."),
                this.row("Utalás indítás", "", "25 000", "2026.03.20."),
                this.row("Esedékességi szerz.", "0", "0", "2026.01.22."));

        ImportResultDTO result = this.webkincstarImportService.processXls(USER_ID, file);

        assertEquals(3, result.getCreated());
        assertEquals(0, result.getUpdated());
        verify(this.securityTransactionRepository, times(2)).save(any(SecurityTransaction.class));
        verify(this.interestRepository, times(1)).save(any(Interest.class));
    }

    @Test
    void processXls_mapsBuyAndSellAndInterestFields() {
        MockMultipartFile file = this.file(
                this.row("Vétel", "1 000 000", "1 012 842", "2026.01.20."),
                this.row("Eladás", "1 000 000", "1 020 000", "2026.06.30."),
                this.row("Esedékesség fizetés", "1 000 000", "25 000", "2026.03.20."));

        this.webkincstarImportService.processXls(USER_ID, file);

        ArgumentCaptor<SecurityTransaction> transactions = ArgumentCaptor.forClass(SecurityTransaction.class);
        verify(this.securityTransactionRepository, times(2)).save(transactions.capture());
        SecurityTransaction buy = transactions.getAllValues().get(0);
        assertEquals("B", buy.getBuySell());
        assertEquals(1000000, buy.getQuantity());
        assertEquals(1012842.0, buy.getAmount());
        assertEquals(LocalDate.of(2026, 1, 20), buy.getTransactionDate());
        assertEquals(BOND_ID, buy.getSecurity().getId());
        assertEquals("HUF", buy.getCurrency().getId());
        assertEquals(USER_ID, buy.getUser().getId());
        assertEquals("S", transactions.getAllValues().get(1).getBuySell());

        ArgumentCaptor<Interest> interests = ArgumentCaptor.forClass(Interest.class);
        verify(this.interestRepository).save(interests.capture());
        assertEquals(25000.0, interests.getValue().getAmount());
        assertEquals(LocalDate.of(2026, 3, 20), interests.getValue().getInterestDate());
        assertEquals(BOND_ID, interests.getValue().getSecurity().getId());
    }

    @Test
    void processXls_readsNominalValueFromNumericCell() {
        MockMultipartFile file = this.file(this.row("Vétel", 89.0, 89.21, "2026.08.27."));

        this.webkincstarImportService.processXls(USER_ID, file);

        ArgumentCaptor<SecurityTransaction> transactions = ArgumentCaptor.forClass(SecurityTransaction.class);
        verify(this.securityTransactionRepository).save(transactions.capture());
        assertEquals(89, transactions.getValue().getQuantity());
        assertEquals(89.21, transactions.getValue().getAmount());
    }

    @Test
    void processXls_updatesInsteadOfDuplicatingOnReimport() {
        when(this.securityTransactionRepository
                .findByUserIdAndSecurity_IdAndTransactionDateAndBuySellAndQuantityAndAmountOrderById(
                        anyLong(), anyString(), any(), anyString(), anyInt(), any()))
                .thenReturn(List.of(SecurityTransaction.builder().id(10L).build()));
        when(this.interestRepository
                .findByUserIdAndSecurity_IdAndInterestDateAndAmountAndCurrency_IdOrderById(
                        anyLong(), anyString(), any(), any(), anyString()))
                .thenReturn(List.of(Interest.builder().id(20L).build()));
        MockMultipartFile file = this.file(
                this.row("Vétel", "1 000 000", "1 012 842", "2026.01.20."),
                this.row("Esedékesség fizetés", "1 000 000", "25 000", "2026.03.20."));

        ImportResultDTO result = this.webkincstarImportService.processXls(USER_ID, file);

        assertEquals(0, result.getCreated());
        assertEquals(2, result.getUpdated());
        verify(this.securityTransactionRepository, never()).save(any(SecurityTransaction.class));
        verify(this.interestRepository, never()).save(any(Interest.class));
    }

    @Test
    void processXls_identicalRowsAdoptDistinctExistingRows() {
        when(this.securityTransactionRepository
                .findByUserIdAndSecurity_IdAndTransactionDateAndBuySellAndQuantityAndAmountOrderById(
                        anyLong(), anyString(), any(), anyString(), anyInt(), any()))
                .thenReturn(List.of(SecurityTransaction.builder().id(10L).build(),
                        SecurityTransaction.builder().id(11L).build()));
        MockMultipartFile file = this.file(
                this.row("Vétel", "1 000 000", "1 012 842", "2026.01.20."),
                this.row("Vétel", "1 000 000", "1 012 842", "2026.01.20."));

        ImportResultDTO result = this.webkincstarImportService.processXls(USER_ID, file);

        assertEquals(0, result.getCreated());
        assertEquals(2, result.getUpdated());
        verify(this.securityTransactionRepository, never()).save(any(SecurityTransaction.class));
    }

    @Test
    void processXls_createsSecondRowWhenOnlyOneExistingMatch() {
        when(this.securityTransactionRepository
                .findByUserIdAndSecurity_IdAndTransactionDateAndBuySellAndQuantityAndAmountOrderById(
                        anyLong(), anyString(), any(), anyString(), anyInt(), any()))
                .thenReturn(List.of(SecurityTransaction.builder().id(10L).build()));
        MockMultipartFile file = this.file(
                this.row("Vétel", "1 000 000", "1 012 842", "2026.01.20."),
                this.row("Vétel", "1 000 000", "1 012 842", "2026.01.20."));

        ImportResultDTO result = this.webkincstarImportService.processXls(USER_ID, file);

        assertEquals(1, result.getCreated());
        assertEquals(1, result.getUpdated());
        verify(this.securityTransactionRepository, times(1)).save(any(SecurityTransaction.class));
    }

    @Test
    void processXls_skipsZeroQuantityTransaction() {
        MockMultipartFile file = this.file(this.row("Vétel", "0", "0", "2026.01.22."));

        ImportResultDTO result = this.webkincstarImportService.processXls(USER_ID, file);

        assertEquals(0, result.getCreated());
        assertEquals(0, result.getUpdated());
        verify(this.securityTransactionRepository, never()).save(any(SecurityTransaction.class));
    }

    @Test
    void processXls_throwsOnUnknownInstrument() {
        when(this.securityService.getOrCreateByName(BOND_NAME))
                .thenThrow(new CSVException("Unknown instrument: " + BOND_NAME));
        MockMultipartFile file = this.file(this.row("Vétel", "1 000 000", "1 012 842", "2026.01.20."));

        CSVException exception = assertThrows(CSVException.class,
                () -> this.webkincstarImportService.processXls(USER_ID, file));

        assertEquals("Unknown instrument: " + BOND_NAME, exception.getMessage());
    }

    @Test
    void processXls_throwsOnUnknownCurrency() {
        when(this.currencyRepository.findById("HUF")).thenReturn(Optional.empty());
        MockMultipartFile file = this.file(this.row("Vétel", "1 000 000", "1 012 842", "2026.01.20."));

        CSVException exception = assertThrows(CSVException.class,
                () -> this.webkincstarImportService.processXls(USER_ID, file));

        assertEquals("Unknown currency: HUF", exception.getMessage());
    }

    @Test
    void processXls_throwsOnMissingHeader() {
        String[] headers = {"Számlaszám", "Számla típusa", "Tranzakció típusa", "Tranzakció azonosítója",
                "Instrumentum", "Tranzakció státusza", "Névérték", "Devizanem", "Értéknap"};
        MockMultipartFile file = this.file(headers,
                new Object[]{ACCOUNT, "", "Vétel", "TST0000000000001", BOND_NAME, "Lezárt", "1 000 000",
                        "HUF", "2026.01.20."});

        CSVException exception = assertThrows(CSVException.class,
                () -> this.webkincstarImportService.processXls(USER_ID, file));

        assertEquals("Missing column: Összeg", exception.getMessage());
    }

    @Test
    void processXls_throwsOnUnparsableDate() {
        MockMultipartFile file = this.file(this.row("Vétel", "1 000 000", "1 012 842", "20th of January"));

        CSVException exception = assertThrows(CSVException.class,
                () -> this.webkincstarImportService.processXls(USER_ID, file));

        assertEquals("Failed to parse the Excel file at row 2", exception.getMessage());
    }
}
