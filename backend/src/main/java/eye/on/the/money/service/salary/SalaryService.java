package eye.on.the.money.service.salary;

import eye.on.the.money.dto.in.SalaryEditDTO;
import eye.on.the.money.dto.out.SalaryDTO;
import eye.on.the.money.dto.out.SalaryNetDTO;
import eye.on.the.money.dto.out.SalaryRaiseDTO;
import eye.on.the.money.dto.out.SalaryRaiseScenarioDTO;
import eye.on.the.money.exception.ValidationException;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.salary.Salary;
import eye.on.the.money.model.salary.SalaryBasis;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.salary.SalaryRepository;
import eye.on.the.money.service.shared.SalaryTaxCalculator;
import eye.on.the.money.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SalaryService {

    private static final BigDecimal MONTHS_IN_YEAR = BigDecimal.valueOf(12);
    private static final BigDecimal PERCENT = BigDecimal.valueOf(100);
    private static final List<BigDecimal> RAISE_PERCENTS =
            List.of(new BigDecimal("2"), new BigDecimal("5"), new BigDecimal("10"),
                    new BigDecimal("20"), new BigDecimal("25"));

    private final SalaryRepository salaryRepository;
    private final CurrencyRepository currencyRepository;
    private final SalaryTaxCalculator salaryTaxCalculator;
    private final UserService userService;

    public List<SalaryDTO> getSalaries(Long userId) {
        return this.salaryRepository.findByUserIdOrderByValidFromDesc(userId)
                .stream().map(this::convertToDTO).toList();
    }

    public Optional<SalaryRaiseDTO> getRaiseScenarios(Long userId) {
        List<Salary> salaries = this.salaryRepository.findByUserIdOrderByValidFromDesc(userId);

        return this.currentSalary(salaries).map(salary -> SalaryRaiseDTO.builder()
                .current(this.convertToDTO(salary))
                .scenarios(RAISE_PERCENTS.stream().map(percent -> this.scenario(percent, salary)).toList())
                .build());
    }

    @Transactional
    public SalaryDTO createSalary(Long userId, SalaryEditDTO editDTO) {
        this.rejectInvalidPeriod(editDTO);

        Salary salary = Salary.builder()
                .amount(editDTO.amount())
                .basis(editDTO.basis())
                .validFrom(editDTO.validFrom())
                .validTo(editDTO.validTo())
                .dependents(editDTO.dependents())
                .note(this.trimToNull(editDTO.note()))
                .currency(this.findCurrency(editDTO.currencyId()))
                .user(this.userService.getReference(userId))
                .build();

        return this.convertToDTO(this.salaryRepository.saveAndFlush(salary));
    }

    @Transactional
    public SalaryDTO updateSalary(Long userId, Long id, SalaryEditDTO editDTO) {
        this.rejectInvalidPeriod(editDTO);

        Salary salary = this.salaryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Salary not found: " + id));

        salary.setAmount(editDTO.amount());
        salary.setBasis(editDTO.basis());
        salary.setValidFrom(editDTO.validFrom());
        salary.setValidTo(editDTO.validTo());
        salary.setDependents(editDTO.dependents());
        salary.setNote(this.trimToNull(editDTO.note()));
        salary.setCurrency(this.findCurrency(editDTO.currencyId()));

        return this.convertToDTO(this.salaryRepository.saveAndFlush(salary));
    }

    @Transactional
    public void deleteSalariesByIds(Long userId, List<Long> ids) {
        this.salaryRepository.deleteByUserIdAndIdIn(userId, ids);
    }

    private Optional<Salary> currentSalary(List<Salary> salaries) {
        return salaries.stream().filter(salary -> salary.getValidTo() == null).findFirst()
                .or(() -> salaries.stream().findFirst());
    }

    private SalaryRaiseScenarioDTO scenario(BigDecimal percent, Salary salary) {
        String currencyId = salary.getCurrency().getId();
        BigDecimal raised = this.grossMonthly(salary)
                .multiply(BigDecimal.ONE.add(percent.divide(PERCENT, 4, RoundingMode.HALF_UP)));
        BigDecimal grossMonthly = this.salaryTaxCalculator.round(raised, currencyId);
        SalaryNetDTO net = this.salaryTaxCalculator.calculate(grossMonthly, currencyId, salary.getDependents());

        return SalaryRaiseScenarioDTO.builder()
                .percent(percent)
                .grossMonthly(grossMonthly)
                .grossAnnual(grossMonthly.multiply(MONTHS_IN_YEAR))
                .netMonthly(net.getNetMonthly())
                .netAnnual(net.getNetMonthly().multiply(MONTHS_IN_YEAR))
                .build();
    }

    private void rejectInvalidPeriod(SalaryEditDTO editDTO) {
        if (editDTO.validTo() != null && editDTO.validTo().isBefore(editDTO.validFrom())) {
            throw new ValidationException("The end of the period cannot be earlier than its start");
        }
    }

    private Currency findCurrency(String currencyId) {
        return this.currencyRepository.findById(currencyId)
                .orElseThrow(() -> new NoSuchElementException("Currency not found: " + currencyId));
    }

    private String trimToNull(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private SalaryDTO convertToDTO(Salary salary) {
        String currencyId = salary.getCurrency().getId();
        BigDecimal grossMonthly = this.grossMonthly(salary);
        BigDecimal grossAnnual = this.grossAnnual(salary);
        SalaryNetDTO net = this.salaryTaxCalculator.calculate(grossMonthly, currencyId, salary.getDependents());

        return SalaryDTO.builder()
                .id(salary.getId())
                .amount(salary.getAmount())
                .basis(salary.getBasis())
                .currencyId(currencyId)
                .validFrom(salary.getValidFrom())
                .validTo(salary.getValidTo())
                .dependents(salary.getDependents())
                .note(salary.getNote())
                .grossMonthly(grossMonthly)
                .grossAnnual(grossAnnual)
                .netMonthly(net.getNetMonthly())
                .netAnnual(net.getNetMonthly().multiply(MONTHS_IN_YEAR))
                .szjaMonthly(net.getSzjaMonthly())
                .szjaAnnual(net.getSzjaMonthly().multiply(MONTHS_IN_YEAR))
                .tbMonthly(net.getTbMonthly())
                .tbAnnual(net.getTbMonthly().multiply(MONTHS_IN_YEAR))
                .familyAllowanceMonthly(net.getFamilyAllowanceMonthly())
                .familyAllowanceApplied(net.isFamilyAllowanceApplied())
                .build();
    }

    private BigDecimal grossMonthly(Salary salary) {
        return SalaryBasis.ANNUAL == salary.getBasis()
                ? salary.getAmount().divide(MONTHS_IN_YEAR, 2, RoundingMode.HALF_UP)
                : salary.getAmount();
    }

    private BigDecimal grossAnnual(Salary salary) {
        return SalaryBasis.ANNUAL == salary.getBasis()
                ? salary.getAmount()
                : salary.getAmount().multiply(MONTHS_IN_YEAR);
    }
}
