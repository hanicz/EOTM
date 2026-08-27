package eye.on.the.money.service.etf;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.exception.CSVException;
import eye.on.the.money.model.User;
import eye.on.the.money.model.etf.ETFInvestment;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.repository.etf.ETFInvestmentRepository;
import eye.on.the.money.service.api.EODAPIService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class ETFInvestmentServiceTest {

    private static final String CSV_HEADER =
            "Investment Id,Quantity,Type,Transaction Date,Short Name,Exchange,Amount,Currency,Fee,Account\n";

    @Autowired
    private ETFInvestmentRepository etfInvestmentRepository;
    @Autowired
    private ETFInvestmentService etfInvestmentService;
    @Autowired
    private UserRepository userRepository;
    @MockitoBean
    private EODAPIService eodAPIService;
    private User user;

    @BeforeEach
    public void init() {
        this.user = this.userRepository.findByEmail("test@test.test");
    }

    @Test
    public void getETFInvestmentsByAccountId() {
        List<ETFInvestmentDTO> result = this.etfInvestmentService.getETFInvestmentsByAccountId(this.user.getUsername(), 2L);

        assertEquals(1, result.size());
        assertEquals("VWRL", result.getFirst().getShortName());
        assertEquals(2L, result.getFirst().getAccountId());
        assertEquals("ACCOUNT 2", result.getFirst().getAccountName());
    }

    @Test
    public void getAllPositions_keepsTheSameEtfInDifferentAccountsApart() {
        List<ETFInvestmentDTO> result = this.etfInvestmentService.getAllPositions(this.user.getUsername());

        List<ETFInvestmentDTO> vwrl = result.stream().filter(i -> "VWRL".equals(i.getShortName())).toList();
        assertEquals(2, vwrl.size());
        assertEquals(500.0, vwrl.stream().filter(i -> i.getAccountId() == 1L).findFirst().orElseThrow().getAmount());
        assertEquals(260.0, vwrl.stream().filter(i -> i.getAccountId() == 2L).findFirst().orElseThrow().getAmount());
    }

    @Test
    public void getPositionsByAccountId_onlyReturnsThatAccount() {
        List<ETFInvestmentDTO> result = this.etfInvestmentService.getPositionsByAccountId(this.user.getUsername(), 2L);

        assertEquals(1, result.size());
        assertEquals("VWRL", result.getFirst().getShortName());
        assertEquals(260.0, result.getFirst().getAmount());
    }

    @Test
    public void getPositionsByAccountId_mergesTheBuyAndSellOfAClosedPosition() {
        List<ETFInvestmentDTO> result = this.etfInvestmentService.getPositionsByAccountId(this.user.getUsername(), 1L);

        ETFInvestmentDTO vwce = result.stream().filter(i -> "VWCE".equals(i.getShortName())).findFirst().orElseThrow();
        assertEquals(0, vwce.getQuantity());
        assertEquals(-40.0, vwce.getAmount());
    }

    @Test
    @Transactional
    public void getPositionsByAccountId_keepsTheSameTickerOnDifferentExchangesApart() {
        this.etfInvestmentService.createInvestment(ETFInvestmentDTO.builder()
                .buySell("B").quantity(10).amount(1000.0).currencyId("EUR").fee(0.0)
                .shortName("VWCE").exchange("MI").accountId(2L)
                .transactionDate(LocalDate.of(2024, 1, 10)).build(), this.user.getUsername());
        this.etfInvestmentService.createInvestment(ETFInvestmentDTO.builder()
                .buySell("B").quantity(4).amount(700.0).currencyId("EUR").fee(0.0)
                .shortName("VWCE").exchange("XETRA").accountId(2L)
                .transactionDate(LocalDate.of(2024, 1, 11)).build(), this.user.getUsername());

        List<ETFInvestmentDTO> vwce = this.etfInvestmentService
                .getPositionsByAccountId(this.user.getUsername(), 2L).stream()
                .filter(i -> "VWCE".equals(i.getShortName())).toList();

        Assertions.assertAll("Two exchanges stay two positions",
                () -> assertEquals(2, vwce.size()),
                () -> assertEquals(1000.0, vwce.stream().filter(i -> "MI".equals(i.getExchange()))
                        .findFirst().orElseThrow().getAmount()),
                () -> assertEquals(700.0, vwce.stream().filter(i -> "XETRA".equals(i.getExchange()))
                        .findFirst().orElseThrow().getAmount()));
    }

    @Test
    @Transactional
    public void createInvestment_storesAmountCurrencyAndAccountOnTheInvestment() {
        ETFInvestmentDTO created = this.etfInvestmentService.createInvestment(ETFInvestmentDTO.builder()
                .buySell("B").quantity(3).amount(123.45).currencyId("EUR").fee(1.0)
                .shortName("VWCE").exchange("MI").accountId(2L)
                .transactionDate(LocalDate.of(2023, 9, 1)).build(), this.user.getUsername());

        ETFInvestment stored = this.etfInvestmentRepository.findByIdAndUserEmail(created.getId(), this.user.getUsername()).orElseThrow();

        Assertions.assertAll("Assert stored ETF investment",
                () -> assertEquals(123.45, stored.getAmount()),
                () -> assertEquals("EUR", stored.getCurrency().getId()),
                () -> assertEquals(2L, stored.getAccount().getId()),
                () -> assertEquals("ACCOUNT 2", created.getAccountName()),
                () -> assertEquals(2L, created.getAccountId())
        );
    }

    @Test
    @Transactional
    public void createInvestment_rejectsAnAccountTheUserDoesNotOwn() {
        ETFInvestmentDTO dto = ETFInvestmentDTO.builder()
                .buySell("B").quantity(3).amount(123.45).currencyId("EUR").fee(1.0)
                .shortName("VWCE").exchange("MI").accountId(999L)
                .transactionDate(LocalDate.of(2023, 9, 1)).build();

        assertThrows(NoSuchElementException.class,
                () -> this.etfInvestmentService.createInvestment(dto, this.user.getUsername()));
    }

    @Test
    @Transactional
    public void updateInvestment_movesTheInvestmentToAnotherAccount() {
        ETFInvestment existing = this.etfInvestmentRepository.findByUserEmailAndAccountIdOrderByTransactionDateDesc(
                this.user.getUsername(), 2L).getFirst();

        this.etfInvestmentService.updateInvestment(ETFInvestmentDTO.builder()
                .id(existing.getId()).buySell("B").quantity(5).amount(300.0).currencyId("EUR").fee(1.5)
                .shortName("VWRL").exchange("AS").accountId(1L)
                .transactionDate(existing.getTransactionDate()).build(), this.user.getUsername());

        ETFInvestment moved = this.etfInvestmentRepository.findByIdAndUserEmail(existing.getId(), this.user.getUsername()).orElseThrow();

        assertEquals(1L, moved.getAccount().getId());
        assertEquals(300.0, moved.getAmount());
    }

    @Test
    @Transactional
    public void processCSV_resolvesTheAccountByName() {
        String csv = CSV_HEADER + ",7,B,2023-10-01,VWCE,MI,700.0,EUR,2.0,ACCOUNT 2\n";

        this.etfInvestmentService.processCSV(this.user.getUsername(), this.csvFile(csv));

        List<ETFInvestment> inAccountTwo = this.etfInvestmentRepository
                .findByUserEmailAndAccountIdOrderByTransactionDateDesc(this.user.getUsername(), 2L);
        assertTrue(inAccountTwo.stream().anyMatch(i -> i.getAmount() == 700.0 && "VWCE".equals(i.getEtf().getShortName())));
    }

    @Test
    @Transactional
    public void processCSV_rejectsAnUnknownAccount() {
        String csv = CSV_HEADER + ",7,B,2023-10-01,VWCE,MI,700.0,EUR,2.0,NO SUCH ACCOUNT\n";

        assertThrows(CSVException.class,
                () -> this.etfInvestmentService.processCSV(this.user.getUsername(), this.csvFile(csv)));
    }

    @Test
    @Transactional
    public void processCSV_rejectsAMissingAccount() {
        String csv = CSV_HEADER + ",7,B,2023-10-01,VWCE,MI,700.0,EUR,2.0,\n";

        assertThrows(CSVException.class,
                () -> this.etfInvestmentService.processCSV(this.user.getUsername(), this.csvFile(csv)));
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "etf.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
}
