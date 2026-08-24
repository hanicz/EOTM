package eye.on.the.money.service.financial;

import eye.on.the.money.dto.in.BankTransactionEditDTO;
import eye.on.the.money.dto.out.BankTransactionDTO;
import eye.on.the.money.dto.out.ImportResultDTO;
import eye.on.the.money.exception.CSVException;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.financial.BankTransaction;
import eye.on.the.money.repository.financial.BankTransactionRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BankTransactionServiceTest {

    private static final String USER_EMAIL = "test@email.com";
    private static final String BANK_ID = "AAACT253654SV4TMYZ";
    private static final String ACCOUNT = "104040278676776881541004";
    private static final LocalDate BOOKING_DATE = LocalDate.of(2025, 12, 31);

    @Mock
    private BankTransactionRepository bankTransactionRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private UserService userService;
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private BankTransactionService bankTransactionService;

    private final User user = User.builder().id(1L).email(USER_EMAIL).build();
    private final Currency huf = new Currency("HUF", "forint");

    @BeforeEach
    void setUp() {
        when(this.userService.loadUserByEmail(USER_EMAIL)).thenReturn(this.user);
        when(this.currencyRepository.findById("HUF")).thenReturn(Optional.of(this.huf));
        when(this.bankTransactionRepository
                .findByUserEmailAndBankTransactionIdAndBookingDateAndTypeAndAmountAndMemo(
                        anyString(), anyString(), any(), anyString(), any(), anyString()))
                .thenReturn(Optional.empty());
    }

    private String row(String date, String bankId, String type, String amount, String memo) {
        String[] fields = new String[BankTransactionDTO.KH_HEADERS.length];
        Arrays.fill(fields, "");
        fields[0] = date;
        fields[1] = bankId;
        fields[2] = type;
        fields[3] = ACCOUNT;
        fields[4] = "HANICZ TAMAS";
        fields[7] = amount;
        fields[8] = "HUF";
        fields[9] = memo;
        return String.join("\t", fields);
    }

    private String blankRow() {
        String[] fields = new String[BankTransactionDTO.KH_HEADERS.length];
        Arrays.fill(fields, "");
        return String.join("\t", fields);
    }

    private MockMultipartFile file(Charset charset, String... rows) {
        String content = String.join("\t", BankTransactionDTO.KH_HEADERS) + "\n" + String.join("\n", rows) + "\n";
        return new MockMultipartFile("file", "kh.csv", "text/csv", content.getBytes(charset));
    }

    private BankTransaction captureSingleSave() {
        ArgumentCaptor<BankTransaction> captor = ArgumentCaptor.forClass(BankTransaction.class);
        verify(this.bankTransactionRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void processCSV_createsBothRowsSharingOneBankId() {
        MockMultipartFile file = this.file(StandardCharsets.UTF_8,
                this.row("2025.12.31", BANK_ID, "Mobilinfo uzenetdij", "-275", "Ref.: " + BANK_ID + " 5 uz."),
                this.row("2025.12.31", BANK_ID, "Kamatado", "-5", "Ref.: " + BANK_ID));

        ImportResultDTO result = this.bankTransactionService.processCSV(USER_EMAIL, file);

        assertEquals(2, result.getCreated());
        assertEquals(0, result.getUpdated());

        ArgumentCaptor<BankTransaction> captor = ArgumentCaptor.forClass(BankTransaction.class);
        verify(this.bankTransactionRepository, times(2)).save(captor.capture());

        List<BankTransaction> saved = captor.getAllValues();
        assertEquals(BOOKING_DATE, saved.get(0).getBookingDate());
        assertEquals(BANK_ID, saved.get(0).getBankTransactionId());
        assertEquals("Mobilinfo uzenetdij", saved.get(0).getType());
        assertEquals(-275.0, saved.get(0).getAmount());
        assertEquals(ACCOUNT, saved.get(0).getAccountNumber());
        assertEquals("HANICZ TAMAS", saved.get(0).getAccountName());
        assertEquals("", saved.get(0).getPartnerName());
        assertEquals("HUF", saved.get(0).getCurrency().getId());
        assertEquals("Kamatado", saved.get(1).getType());
        assertEquals(-5.0, saved.get(1).getAmount());
    }

    @Test
    void processCSV_updatesInsteadOfDuplicatingOnReimport() {
        BankTransaction existing = BankTransaction.builder().id(7L).bankTransactionId(BANK_ID)
                .bookingDate(BOOKING_DATE).type("Kamatado").amount(-5.0).memo("Ref.")
                .accountNumber("stale").user(this.user).currency(this.huf).build();

        when(this.bankTransactionRepository
                .findByUserEmailAndBankTransactionIdAndBookingDateAndTypeAndAmountAndMemo(
                        USER_EMAIL, BANK_ID, BOOKING_DATE, "Kamatado", -5.0, "Ref."))
                .thenReturn(Optional.of(existing));

        ImportResultDTO result = this.bankTransactionService.processCSV(USER_EMAIL,
                this.file(StandardCharsets.UTF_8, this.row("2025.12.31", BANK_ID, "Kamatado", "-5", "Ref.")));

        assertEquals(0, result.getCreated());
        assertEquals(1, result.getUpdated());
        verify(this.bankTransactionRepository, never()).save(any());
        assertEquals(ACCOUNT, existing.getAccountNumber());
    }

    @Test
    void processCSV_keepsTheExclusionFlagOnReimport() {
        BankTransaction existing = BankTransaction.builder().id(7L).bankTransactionId(BANK_ID)
                .bookingDate(BOOKING_DATE).type("Kamatado").amount(-5.0).memo("Ref.")
                .excluded(true).user(this.user).currency(this.huf).build();

        when(this.bankTransactionRepository
                .findByUserEmailAndBankTransactionIdAndBookingDateAndTypeAndAmountAndMemo(
                        USER_EMAIL, BANK_ID, BOOKING_DATE, "Kamatado", -5.0, "Ref."))
                .thenReturn(Optional.of(existing));

        this.bankTransactionService.processCSV(USER_EMAIL,
                this.file(StandardCharsets.UTF_8, this.row("2025.12.31", BANK_ID, "Kamatado", "-5", "Ref.")));

        assertTrue(existing.isExcluded());
    }

    @Test
    void processCSV_createsANewRowWhenTheMemoWasEdited() {
        BankTransaction edited = BankTransaction.builder().id(7L).bankTransactionId(BANK_ID)
                .bookingDate(BOOKING_DATE).type("Kamatado").amount(-5.0).memo("Rent for December")
                .user(this.user).currency(this.huf).build();

        when(this.bankTransactionRepository
                .findByUserEmailAndBankTransactionIdAndBookingDateAndTypeAndAmountAndMemo(
                        USER_EMAIL, BANK_ID, BOOKING_DATE, "Kamatado", -5.0, "Rent for December"))
                .thenReturn(Optional.of(edited));

        ImportResultDTO result = this.bankTransactionService.processCSV(USER_EMAIL,
                this.file(StandardCharsets.UTF_8, this.row("2025.12.31", BANK_ID, "Kamatado", "-5", "Ref.")));

        assertEquals(1, result.getCreated());
        assertEquals(0, result.getUpdated());
        assertEquals("Ref.", this.captureSingleSave().getMemo());
        assertEquals("Rent for December", edited.getMemo());
    }

    @Test
    void updateTransaction_savesTheBookingDateAndTheTrimmedMemo() {
        BankTransaction existing = BankTransaction.builder().id(7L).bookingDate(BOOKING_DATE).memo("Ref.")
                .user(this.user).currency(this.huf).build();
        when(this.bankTransactionRepository.findByIdAndUserEmail(7L, USER_EMAIL))
                .thenReturn(Optional.of(existing));

        this.bankTransactionService.updateTransaction(USER_EMAIL, 7L,
                new BankTransactionEditDTO(LocalDate.of(2026, 1, 15), "  Rent for December  "));

        assertEquals(LocalDate.of(2026, 1, 15), existing.getBookingDate());
        assertEquals("Rent for December", existing.getMemo());
        verify(this.bankTransactionRepository).save(existing);
    }

    @Test
    void updateTransaction_throwsWhenTheTransactionBelongsToSomeoneElse() {
        when(this.bankTransactionRepository.findByIdAndUserEmail(7L, USER_EMAIL))
                .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> this.bankTransactionService.updateTransaction(USER_EMAIL, 7L,
                new BankTransactionEditDTO(BOOKING_DATE, "Rent")));
        verify(this.bankTransactionRepository, never()).save(any());
    }

    @Test
    void processCSV_createsRecordsIncluded() {
        this.bankTransactionService.processCSV(USER_EMAIL, this.file(StandardCharsets.UTF_8,
                this.row("2025.12.31", BANK_ID, "Kamatado", "-5", "memo")));

        assertFalse(this.captureSingleSave().isExcluded());
    }

    @Test
    void processCSV_parsesHungarianNumberFormat() {
        this.bankTransactionService.processCSV(USER_EMAIL, this.file(StandardCharsets.UTF_8,
                this.row("2025.01.02", BANK_ID, "Utalas", "-1.234.567,89", "memo")));

        assertEquals(-1234567.89, this.captureSingleSave().getAmount());
    }

    @Test
    void processCSV_fallsBackToLatin2ForAccentedText() {
        String accented = "Kamatad" + (char) 0x00F3;

        this.bankTransactionService.processCSV(USER_EMAIL, this.file(Charset.forName("ISO-8859-2"),
                this.row("2025.12.31", BANK_ID, accented, "-5", "memo")));

        assertEquals(accented, this.captureSingleSave().getType());
    }

    @Test
    void processCSV_readsUtf8AccentedText() {
        String accented = "Kamatad" + (char) 0x00F3;

        this.bankTransactionService.processCSV(USER_EMAIL, this.file(StandardCharsets.UTF_8,
                this.row("2025.12.31", BANK_ID, accented, "-5", "memo")));

        assertEquals(accented, this.captureSingleSave().getType());
    }

    @Test
    void processCSV_skipsBlankRows() {
        ImportResultDTO result = this.bankTransactionService.processCSV(USER_EMAIL, this.file(StandardCharsets.UTF_8,
                this.row("2025.12.31", BANK_ID, "Kamatado", "-5", "memo"), this.blankRow()));

        assertEquals(1, result.getCreated());
        verify(this.bankTransactionRepository, times(1)).save(any());
    }

    @Test
    void processCSV_truncatesOverlongMemo() {
        String longMemo = "x".repeat(BankTransaction.MEMO_MAX_LENGTH + 50);

        this.bankTransactionService.processCSV(USER_EMAIL, this.file(StandardCharsets.UTF_8,
                this.row("2025.12.31", BANK_ID, "Kamatado", "-5", longMemo)));

        assertEquals(BankTransaction.MEMO_MAX_LENGTH, this.captureSingleSave().getMemo().length());
    }

    @Test
    void processCSV_rejectsUnknownCurrency() {
        when(this.currencyRepository.findById("HUF")).thenReturn(Optional.empty());

        assertThrows(CSVException.class, () -> this.bankTransactionService.processCSV(USER_EMAIL,
                this.file(StandardCharsets.UTF_8, this.row("2025.12.31", BANK_ID, "Kamatado", "-5", "memo"))));
    }

    @Test
    void processCSV_rejectsMalformedDate() {
        assertThrows(CSVException.class, () -> this.bankTransactionService.processCSV(USER_EMAIL,
                this.file(StandardCharsets.UTF_8, this.row("31/12/2025", BANK_ID, "Kamatado", "-5", "memo"))));
    }

    @Test
    void getTransactions_returnsMappedDTOs() {
        BankTransaction transaction = BankTransaction.builder().id(1L).amount(-275.0).build();
        BankTransactionDTO dto = BankTransactionDTO.builder().id(1L).amount(-275.0).build();
        when(this.bankTransactionRepository.findByUserEmailOrderByBookingDateDesc(USER_EMAIL)).thenReturn(List.of(transaction));
        when(this.modelMapper.map(transaction, BankTransactionDTO.class)).thenReturn(dto);

        List<BankTransactionDTO> result = this.bankTransactionService.getTransactions(USER_EMAIL);

        assertEquals(1, result.size());
        assertEquals(-275.0, result.getFirst().getAmount());
    }
}
