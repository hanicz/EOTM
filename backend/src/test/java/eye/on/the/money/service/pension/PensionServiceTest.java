package eye.on.the.money.service.pension;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.PensionDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.repository.pension.PensionRepository;
import eye.on.the.money.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
class PensionServiceTest {

    @Autowired
    private PensionService pensionService;
    @Autowired
    private PensionRepository pensionRepository;
    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    public void init() {
        this.user = this.userRepository.findByEmail("test@test.test");
        this.pensionRepository.findByUserId(this.user.getId()).ifPresent(this.pensionRepository::delete);
    }

    @Test
    void getPension_ReturnsZeroedDefaultWhenNothingSaved() {
        PensionDTO pension = this.pensionService.getPension(this.user.getId());

        assertDecimal("0", pension.getTotalContribution());
        assertDecimal("0", pension.getCurrentValue());
        Assertions.assertEquals(PensionService.DEFAULT_CURRENCY, pension.getCurrency());
    }

    @Test
    void updatePension_CreatesTheRowOnFirstSave() {
        PensionDTO saved = this.pensionService.updatePension(this.user.getId(), PensionDTO.builder()
                .totalContribution(new BigDecimal("1500000")).currentValue(new BigDecimal("1800000")).currency("HUF").build());

        assertDecimal("1500000", saved.getTotalContribution());
        assertDecimal("1800000", saved.getCurrentValue());
        Assertions.assertEquals("HUF", saved.getCurrency());
        Assertions.assertTrue(this.pensionRepository.findByUserId(this.user.getId()).isPresent());
    }

    @Test
    void updatePension_OverwritesTheExistingRowRatherThanAddingAnother() {
        this.pensionService.updatePension(this.user.getId(), PensionDTO.builder()
                .totalContribution(new BigDecimal("1500000")).currentValue(new BigDecimal("1800000")).currency("HUF").build());

        PensionDTO updated = this.pensionService.updatePension(this.user.getId(), PensionDTO.builder()
                .totalContribution(new BigDecimal("1650000")).currentValue(new BigDecimal("2050000")).currency("EUR").build());

        assertDecimal("1650000", updated.getTotalContribution());
        assertDecimal("2050000", updated.getCurrentValue());
        Assertions.assertEquals("EUR", updated.getCurrency());
        Assertions.assertEquals(1, this.pensionRepository.findAll().stream()
                .filter(pension -> pension.getUser().getId().equals(this.user.getId())).count());
    }

    @Test
    void updatePension_FallsBackToTheDefaultCurrencyWhenNoneIsGiven() {
        PensionDTO saved = this.pensionService.updatePension(this.user.getId(), PensionDTO.builder()
                .totalContribution(new BigDecimal("200000")).currentValue(new BigDecimal("215000")).build());

        Assertions.assertEquals(PensionService.DEFAULT_CURRENCY, saved.getCurrency());
    }

    @Test
    void updatePension_RejectsAnUnknownCurrency() {
        PensionDTO pension = PensionDTO.builder()
                .totalContribution(new BigDecimal("200000")).currentValue(new BigDecimal("215000")).currency("XYZ").build();

        Assertions.assertThrows(NoSuchElementException.class,
                () -> this.pensionService.updatePension(this.user.getId(), pension));
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        Assertions.assertEquals(0, new BigDecimal(expected).compareTo(actual), () -> "expected " + expected + " but was " + actual);
    }
}
