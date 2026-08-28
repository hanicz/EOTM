package eye.on.the.money.service.shared;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.exception.CSVException;
import eye.on.the.money.model.User;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.service.stock.DividendService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
public class CSVErrorMessageTest {

    private static final String HEADER = "Dividend Id,Amount,Dividend Date,Short Name,Exchange,Currency\n";

    @Autowired
    private DividendService dividendService;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    public void init() {
        this.user = this.userRepository.findByEmail("test@test.test");
    }

    private CSVException importAndCatch(String csvContent) {
        MultipartFile file = new MockMultipartFile("file", "file.csv", MediaType.TEXT_PLAIN_VALUE, csvContent.getBytes());
        return assertThrows(CSVException.class, () -> this.dividendService.processCSV(this.user.getId(), file));
    }

    private void assertNoInternalDetail(String message) {
        Assertions.assertFalse(message.contains("For input string"), message);
        Assertions.assertFalse(message.contains("CSVRecord only has"), message);
        Assertions.assertFalse(message.contains("could not be parsed at index"), message);
        Assertions.assertFalse(message.contains("Index for header"), message);
    }

    @Test
    public void badDateReportsPhysicalLineNumber() {
        CSVException e = this.importAndCatch(HEADER
                + ",299.0,2024-01-01,INTC,US,USD\n"
                + ",299.0,NOT_DATE,INTC,US,USD");

        Assertions.assertEquals("Failed to parse CSV file at row 3", e.getMessage());
        this.assertNoInternalDetail(e.getMessage());
    }

    @Test
    public void badNumberReportsPhysicalLineNumber() {
        CSVException e = this.importAndCatch(HEADER + ",NOT_A_NUMBER,2024-01-01,INTC,US,USD");

        Assertions.assertEquals("Failed to parse CSV file at row 2", e.getMessage());
        this.assertNoInternalDetail(e.getMessage());
    }

    @Test
    public void shortRowReportsPhysicalLineNumber() {
        CSVException e = this.importAndCatch("EXCEPTION,1\n3,EXC,333\n64");

        this.assertNoInternalDetail(e.getMessage());
        Assertions.assertTrue(e.getMessage().startsWith("Failed to parse CSV file"), e.getMessage());
    }

    @Test
    public void validationExceptionInsideImportIsWrappedWithLineNumber() {
        CSVException e = this.importAndCatch(HEADER + ",299.0,2024-01-01,INTC, ,USD");

        Assertions.assertEquals("Failed to parse CSV file at row 2", e.getMessage());
        this.assertNoInternalDetail(e.getMessage());
    }

    @Test
    public void validationMessagesArePreserved() {
        MultipartFile file = new MockMultipartFile("file", "file.csv", MediaType.TEXT_PLAIN_VALUE,
                (HEADER + ",299.0,2024-01-01,INTC,US,NOT_A_CURRENCY").getBytes());

        NoSuchElementException e = assertThrows(NoSuchElementException.class,
                () -> this.dividendService.processCSV(this.user.getId(), file));

        Assertions.assertEquals("Currency not found: NOT_A_CURRENCY", e.getMessage());
    }
}
